package com.oscan.android.data.storage

import android.content.Context
import java.io.File
import java.io.InputStream

enum class AssetKind(val directory: String) {
    ORIGINAL("originals"),
    PROCESSED("processed"),
    THUMBNAIL("thumbnails"),
    SESSION("sessions")
}

class DocumentFileStore(context: Context) {
    private val root = File(context.filesDir, "documents")

    fun write(
        documentId: String,
        pageId: String,
        kind: AssetKind,
        extension: String,
        source: InputStream
    ): String {
        require(documentId.isSafePathSegment() && pageId.isSafePathSegment())
        val safeExtension = extension.lowercase().removePrefix(".")
        require(safeExtension.matches(Regex("[a-z0-9]{1,8}")))

        val directory = File(root, "$documentId/${kind.directory}").apply { mkdirs() }
        val destination = File(directory, "$pageId.$safeExtension")
        val temporary = File(directory, ".${destination.name}.writing")
        try {
            temporary.outputStream().buffered().use { output -> source.copyTo(output) }
            if (!temporary.renameTo(destination)) {
                destination.outputStream().use { output -> temporary.inputStream().use { it.copyTo(output) } }
                temporary.delete()
            }
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
        return destination.relativeTo(root).invariantSeparatorsPath
    }

    fun resolve(relativePath: String): File {
        val candidate = File(root, relativePath).canonicalFile
        require(candidate.path.startsWith(root.canonicalFile.path + File.separator))
        return candidate
    }

    fun deleteDocument(documentId: String): Boolean {
        require(documentId.isSafePathSegment())
        val directory = File(root, documentId)
        return !directory.exists() || directory.deleteRecursively()
    }

    fun deleteAsset(relativePath: String): Boolean = resolve(relativePath).let { !it.exists() || it.delete() }

    val rootDir: File get() = root

    fun getStorageSize(): Long {
        if (!root.exists()) return 0L
        return root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun clearTempFiles() {
        clearInterruptedWrites()
        if (!root.exists()) return
        root.walkTopDown()
            .filter { it.isFile && (it.name.endsWith(".tmp") || it.name.endsWith(".writing") || it.parentFile?.name == "sessions") }
            .forEach(File::delete)
    }

    fun clearInterruptedWrites() {
        if (!root.exists()) return
        root.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".writing") }
            .forEach(File::delete)
    }

    private fun String.isSafePathSegment(): Boolean =
        isNotBlank() && matches(Regex("[A-Za-z0-9_-]+"))
}
