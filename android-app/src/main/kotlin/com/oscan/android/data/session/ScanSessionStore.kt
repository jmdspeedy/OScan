package com.oscan.android.data.session

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.oscan.core.model.CornerPoints
import com.oscan.core.model.FilterType
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import org.opencv.core.Point

enum class SessionPageStatus {
    IMPORTING,
    DETECTING,
    CROP_REVIEW,
    PROCESSING,
    TREATMENT_REVIEW,
    ACCEPTED,
    FAILED
}

data class SessionPage(
    val id: String,
    val position: Int,
    val status: SessionPageStatus,
    val sourcePath: String? = null,
    val originalExtension: String = "jpg",
    val processedPath: String? = null,
    val thumbnailPath: String? = null,
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val outputWidth: Int = 0,
    val outputHeight: Int = 0,
    val corners: CornerPoints? = null,
    val initialCorners: CornerPoints? = null,
    val isAutoDetected: Boolean = false,
    val filter: FilterType = FilterType.MAGIC,
    val failureMessage: String? = null
)

data class ScanSession(
    val id: String,
    val pages: List<SessionPage>,
    val currentPageId: String? = null,
    val documentName: String = defaultScanDocumentName(),
    val selectedFolderId: String? = null,
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
) {
    val acceptedPages: List<SessionPage>
        get() = pages.filter { it.status == SessionPageStatus.ACCEPTED }.sortedBy { it.position }
}

private val defaultDocumentNameFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm", Locale.ROOT)

internal fun defaultScanDocumentName(
    epochMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): String = "OScan ${defaultDocumentNameFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zoneId))}"

/** Durable, app-private scan draft storage. Metadata never retains picker URIs. */
class ScanSessionStore(
    context: Context,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    private val root = File(context.filesDir, "scan-sessions")
    private val activeMarker = File(root, "active")

    fun create(): ScanSession {
        val session = ScanSession(id = newId(), pages = emptyList())
        save(session)
        return session
    }

    fun newPageId(): String = newId()

    fun loadActive(): ScanSession? {
        val id = activeMarker.takeIf(File::isFile)?.readText()?.trim().orEmpty()
        if (!id.isSafeSegment()) return null
        return runCatching { readMetadata(id) }.getOrNull()?.sanitize()
    }

    fun save(session: ScanSession) {
        require(session.id.isSafeSegment())
        root.mkdirs()
        val directory = sessionDirectory(session.id).apply { mkdirs() }
        writeAtomically(File(directory, "session.json"), session.toJson().toString().byteInputStream())
        writeAtomically(activeMarker, session.id.byteInputStream())
    }

    fun importSource(sessionId: String, pageId: String, source: InputStream, extension: String = "img"): String {
        val safeExtension = extension.lowercase().removePrefix(".")
        require(safeExtension.matches(Regex("[a-z0-9]{1,8}")))
        val destination = pageDirectory(sessionId, pageId).resolve("source.$safeExtension")
        writeAtomically(destination, source)
        return destination.relativeTo(root).invariantSeparatorsPath
    }

    fun writeProcessed(sessionId: String, pageId: String, bitmap: Bitmap): String =
        writeBitmap(sessionId, pageId, "processed.jpg", bitmap, 94)

    fun writeGeneratedSource(sessionId: String, pageId: String, bitmap: Bitmap): String =
        writeBitmap(sessionId, pageId, "source.jpg", bitmap, 96)

    fun writeThumbnail(sessionId: String, pageId: String, bitmap: Bitmap): String =
        writeBitmap(sessionId, pageId, "thumbnail.jpg", bitmap, 82)

    fun resolve(relativePath: String): File {
        val rootFile = root.canonicalFile
        val candidate = File(rootFile, relativePath).canonicalFile
        require(candidate.path.startsWith(rootFile.path + File.separator))
        return candidate
    }

    fun readBitmap(relativePath: String): Bitmap? = BitmapFactory.decodeFile(resolve(relativePath).path)

    fun deletePage(sessionId: String, pageId: String): Boolean {
        val directory = pageDirectory(sessionId, pageId)
        return !directory.exists() || directory.deleteRecursively()
    }

    fun discard(sessionId: String): Boolean {
        require(sessionId.isSafeSegment())
        val deleted = sessionDirectory(sessionId).let { !it.exists() || it.deleteRecursively() }
        if (activeMarker.readTextOrNull()?.trim() == sessionId) activeMarker.delete()
        return deleted
    }

    private fun readMetadata(sessionId: String): ScanSession {
        val file = File(sessionDirectory(sessionId), "session.json")
        return JSONObject(file.readText()).toSession()
    }

    private fun ScanSession.sanitize(): ScanSession {
        val validPages = pages.map { page ->
            val sourceExists = page.sourcePath?.let { runCatching { resolve(it).isFile }.getOrDefault(false) } == true
            val processedExists = page.processedPath?.let { runCatching { resolve(it).isFile }.getOrDefault(false) } == true
            when {
                !sourceExists -> page.copy(
                    status = SessionPageStatus.FAILED,
                    sourcePath = null,
                    processedPath = null,
                    thumbnailPath = null,
                    failureMessage = "The imported image is no longer available."
                )
                page.status == SessionPageStatus.ACCEPTED && !processedExists -> page.copy(
                    status = SessionPageStatus.CROP_REVIEW,
                    processedPath = null,
                    thumbnailPath = null,
                    failureMessage = null
                )
                page.status in setOf(SessionPageStatus.IMPORTING, SessionPageStatus.DETECTING, SessionPageStatus.PROCESSING) ->
                    page.copy(status = SessionPageStatus.CROP_REVIEW, failureMessage = null)
                else -> page
            }
        }.sortedBy { it.position }.mapIndexed { index, page -> page.copy(position = index) }
        val current = currentPageId?.takeIf { id -> validPages.any { it.id == id } }
        return copy(pages = validPages, currentPageId = current)
    }

    private fun pageDirectory(sessionId: String, pageId: String): File {
        require(sessionId.isSafeSegment() && pageId.isSafeSegment())
        return File(sessionDirectory(sessionId), "pages/$pageId").apply { mkdirs() }
    }

    private fun sessionDirectory(sessionId: String): File {
        require(sessionId.isSafeSegment())
        return File(root, sessionId)
    }

    private fun writeBitmap(sessionId: String, pageId: String, name: String, bitmap: Bitmap, quality: Int): String {
        val destination = pageDirectory(sessionId, pageId).resolve(name)
        val temporary = File(destination.parentFile, ".${destination.name}.writing")
        try {
            temporary.outputStream().buffered().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output))
            }
            replace(temporary, destination)
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
        return destination.relativeTo(root).invariantSeparatorsPath
    }

    private fun writeAtomically(destination: File, source: InputStream) {
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, ".${destination.name}.writing")
        try {
            source.use { input -> temporary.outputStream().buffered().use(input::copyTo) }
            replace(temporary, destination)
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
    }

    private fun replace(temporary: File, destination: File) {
        if (destination.exists() && !destination.delete()) error("Could not replace session asset")
        if (!temporary.renameTo(destination)) {
            temporary.inputStream().use { input -> destination.outputStream().use(input::copyTo) }
            temporary.delete()
        }
    }

    private fun ScanSession.toJson() = JSONObject().apply {
        put("version", 1)
        put("id", id)
        put("currentPageId", currentPageId ?: JSONObject.NULL)
        put("documentName", documentName)
        put("selectedFolderId", selectedFolderId ?: JSONObject.NULL)
        put("updatedAtEpochMillis", updatedAtEpochMillis)
        put("pages", JSONArray().apply { pages.sortedBy(SessionPage::position).forEach { put(it.toJson()) } })
    }

    private fun SessionPage.toJson() = JSONObject().apply {
        put("id", id); put("position", position); put("status", status.name)
        put("sourcePath", sourcePath ?: JSONObject.NULL); put("processedPath", processedPath ?: JSONObject.NULL)
        put("originalExtension", originalExtension)
        put("thumbnailPath", thumbnailPath ?: JSONObject.NULL)
        put("sourceWidth", sourceWidth); put("sourceHeight", sourceHeight)
        put("outputWidth", outputWidth); put("outputHeight", outputHeight)
        put("corners", corners?.toJson() ?: JSONObject.NULL)
        put("initialCorners", initialCorners?.toJson() ?: JSONObject.NULL)
        put("isAutoDetected", isAutoDetected); put("filter", filter.name)
        put("failureMessage", failureMessage ?: JSONObject.NULL)
    }

    private fun CornerPoints.toJson() = JSONArray(toArray().flatMap { listOf(it.x, it.y) })

    private fun JSONObject.toSession() = ScanSession(
        id = getString("id"),
        pages = getJSONArray("pages").let { array -> (0 until array.length()).map { array.getJSONObject(it).toPage() } },
        currentPageId = nullableString("currentPageId"),
        documentName = optString("documentName").takeIf(String::isNotBlank) ?: defaultScanDocumentName(),
        selectedFolderId = nullableString("selectedFolderId"),
        updatedAtEpochMillis = optLong("updatedAtEpochMillis", 0L)
    )

    private fun JSONObject.toPage() = SessionPage(
        id = getString("id"), position = getInt("position"), status = SessionPageStatus.valueOf(getString("status")),
        sourcePath = nullableString("sourcePath"), originalExtension = optString("originalExtension", "jpg"),
        processedPath = nullableString("processedPath"),
        thumbnailPath = nullableString("thumbnailPath"), sourceWidth = optInt("sourceWidth"), sourceHeight = optInt("sourceHeight"),
        outputWidth = optInt("outputWidth"), outputHeight = optInt("outputHeight"),
        corners = optJSONArray("corners")?.toCorners(), initialCorners = optJSONArray("initialCorners")?.toCorners(),
        isAutoDetected = optBoolean("isAutoDetected"),
        filter = runCatching { FilterType.valueOf(optString("filter")) }.getOrDefault(FilterType.MAGIC),
        failureMessage = nullableString("failureMessage")
    )

    private fun JSONArray.toCorners(): CornerPoints? = if (length() == 8) CornerPoints.fromArray(
        Array(4) { index -> Point(getDouble(index * 2), getDouble(index * 2 + 1)) }
    ) else null

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key)

    private fun String.isSafeSegment() = isNotBlank() && matches(Regex("[A-Za-z0-9_-]+"))
    private fun File.readTextOrNull(): String? = runCatching { takeIf(File::isFile)?.readText() }.getOrNull()
}
