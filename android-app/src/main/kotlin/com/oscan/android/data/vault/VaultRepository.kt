package com.oscan.android.data.vault

import android.content.Context
import com.oscan.android.data.model.Document
import com.oscan.android.data.model.DocumentId
import com.oscan.android.data.model.FolderId
import com.oscan.android.data.repository.DocumentRepository
import com.oscan.android.data.repository.NewPage
import com.oscan.android.data.storage.AssetKind
import com.oscan.android.data.storage.DocumentFileStore
import com.oscan.android.data.vault.crypto.KdfParameters
import com.oscan.android.data.vault.crypto.PasscodeValidationResult
import com.oscan.android.data.vault.crypto.VaultCrypto
import com.oscan.android.data.vault.journal.TransactionRecord
import com.oscan.android.data.vault.journal.TransactionStatus
import com.oscan.android.data.vault.journal.TransactionType
import com.oscan.android.data.vault.journal.VaultTransactionJournal
import com.oscan.core.model.CornerPoints
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class VaultRepositoryError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidPasscode(val reason: String) : VaultRepositoryError(reason)
    class IncorrectPasscode(val attemptsLeftBeforeDelay: Int) : VaultRepositoryError("Incorrect passcode")
    class LockedOut(val remainingSeconds: Int) : VaultRepositoryError("Too many attempts. Try again in $remainingSeconds seconds.")
    class NotConfigured : VaultRepositoryError("Vault is not configured")
    class DocumentNotFound : VaultRepositoryError("Vault document not found")
    class StorageError(message: String, cause: Throwable? = null) : VaultRepositoryError(message, cause)
}

interface VaultRepository {
    fun isConfigured(): Boolean
    fun getLockoutRemainingSeconds(): Int
    fun setupVault(passcode: String): ActiveVaultKeys
    fun unlock(passcode: String): ActiveVaultKeys
    fun observeDocuments(keys: ActiveVaultKeys): Flow<List<VaultDocument>>
    fun observeTrash(keys: ActiveVaultKeys): Flow<List<VaultDocument>>
    fun getDocument(keys: ActiveVaultKeys, id: String): VaultDocument?
    fun searchDocuments(keys: ActiveVaultKeys, query: String): List<VaultDocument>
    fun createDocument(keys: ActiveVaultKeys, name: String, pages: List<NewPage>): VaultDocument
    fun renameDocument(keys: ActiveVaultKeys, id: String, name: String)
    fun setFavorite(keys: ActiveVaultKeys, id: String, favorite: Boolean)
    fun moveToTrash(keys: ActiveVaultKeys, id: String)
    fun restoreFromTrash(keys: ActiveVaultKeys, id: String)
    fun permanentlyDelete(keys: ActiveVaultKeys, id: String)
    fun emptyTrash(keys: ActiveVaultKeys)
    fun moveDocumentIntoVault(keys: ActiveVaultKeys, doc: Document, fileStore: DocumentFileStore, repository: DocumentRepository): VaultDocument
    fun moveDocumentOutOfVault(keys: ActiveVaultKeys, vaultDocId: String, targetFolderId: FolderId?, fileStore: DocumentFileStore, repository: DocumentRepository): DocumentId
    fun changePasscode(currentPasscode: String, newPasscode: String, keys: ActiveVaultKeys)
    fun resetVault()
    fun disableVault(currentPasscode: String, keys: ActiveVaultKeys, migrateToLibrary: Boolean, fileStore: DocumentFileStore, repository: DocumentRepository)
    fun cleanEphemeralCache()
    fun getDecryptedAssetStream(keys: ActiveVaultKeys, documentId: String, pageId: String, assetKind: String, assetPath: String): InputStream
}

class LocalVaultRepository(
    private val context: Context,
    private val clock: Clock = Clock.systemUTC(),
    private val newId: () -> String = { UUID.randomUUID().toString() }
) : VaultRepository {

    private val vaultDir = File(context.filesDir, "vault")
    private val assetsDir = File(vaultDir, "assets")
    private val configFile = File(vaultDir, "vault_config.json")
    private val manifestFile = File(vaultDir, "manifest.enc")
    private val journal = VaultTransactionJournal(File(vaultDir, "journal"))
    private val cacheDir = File(context.cacheDir, "vault_cache")

    private val documentsFlow = MutableStateFlow<List<VaultDocument>>(emptyList())
    private val trashFlow = MutableStateFlow<List<VaultDocument>>(emptyList())

    init {
        vaultDir.mkdirs()
        assetsDir.mkdirs()
        cacheDir.mkdirs()
        performStartupCleanup()
    }

    private fun performStartupCleanup() {
        journal.performStartupRecovery {
            cleanEphemeralCache()
        }
    }

    override fun isConfigured(): Boolean {
        return configFile.exists() && manifestFile.exists()
    }

    override fun getLockoutRemainingSeconds(): Int {
        val config = loadConfig() ?: return 0
        val now = clock.millis()
        if (config.nextAllowedAttemptEpochMillis > now) {
            return ((config.nextAllowedAttemptEpochMillis - now + 999) / 1000).toInt()
        }
        return 0
    }

    override fun setupVault(passcode: String): ActiveVaultKeys {
        val validation = VaultCrypto.validatePasscode(passcode)
        if (validation is PasscodeValidationResult.Invalid) {
            throw VaultRepositoryError.InvalidPasscode(validation.reason)
        }

        val vaultId = newId()
        val salt = VaultCrypto.generateSalt()
        val kdfParams = KdfParameters(salt = salt)
        val pdk = VaultCrypto.derivePdk(passcode, kdfParams)
        val vmk = VaultCrypto.generateVmk()
        val wrapped = VaultCrypto.wrapVmk(vmk, pdk)

        val config = VaultConfig(
            vaultId = vaultId,
            saltHex = salt.toHex(),
            iterations = kdfParams.iterations,
            memoryKb = kdfParams.memoryKb,
            parallelism = kdfParams.parallelism,
            wrappedVmkNonceHex = wrapped.nonce.toHex(),
            wrappedVmkCiphertextHex = wrapped.ciphertext.toHex()
        )

        saveConfig(config)

        val docSubkey = VaultCrypto.deriveSubkey(vmk, "vault_doc_key")
        val metaSubkey = VaultCrypto.deriveSubkey(vmk, "vault_meta_key")
        val keys = ActiveVaultKeys(vmk, docSubkey, metaSubkey)

        saveManifest(keys, VaultManifest(emptyList()))
        updateFlows(emptyList())

        return keys
    }

    override fun unlock(passcode: String): ActiveVaultKeys {
        val config = loadConfig() ?: throw VaultRepositoryError.NotConfigured()

        val lockoutSeconds = getLockoutRemainingSeconds()
        if (lockoutSeconds > 0) {
            throw VaultRepositoryError.LockedOut(lockoutSeconds)
        }

        val kdfParams = KdfParameters(
            salt = config.saltHex.hexToByteArray(),
            iterations = config.iterations,
            memoryKb = config.memoryKb,
            parallelism = config.parallelism
        )

        val pdk = VaultCrypto.derivePdk(passcode, kdfParams)
        val wrapped = com.oscan.android.data.vault.crypto.WrappedVmkEnvelope(
            nonce = config.wrappedVmkNonceHex.hexToByteArray(),
            ciphertext = config.wrappedVmkCiphertextHex.hexToByteArray()
        )

        val vmk = try {
            VaultCrypto.unwrapVmk(wrapped, pdk)
        } catch (error: Throwable) {
            val newFailures = config.failedAttempts + 1
            val delaySeconds = calculateLockoutDelaySeconds(newFailures)
            val nextAllowed = if (delaySeconds > 0) clock.millis() + (delaySeconds * 1000L) else 0L
            val updatedConfig = config.copy(
                failedAttempts = newFailures,
                nextAllowedAttemptEpochMillis = nextAllowed
            )
            saveConfig(updatedConfig)

            if (delaySeconds > 0) {
                throw VaultRepositoryError.LockedOut(delaySeconds)
            } else {
                val attemptsLeft = (5 - newFailures).coerceAtLeast(0)
                throw VaultRepositoryError.IncorrectPasscode(attemptsLeft)
            }
        }

        if (config.failedAttempts > 0 || config.nextAllowedAttemptEpochMillis > 0) {
            saveConfig(config.copy(failedAttempts = 0, nextAllowedAttemptEpochMillis = 0L))
        }

        val docSubkey = VaultCrypto.deriveSubkey(vmk, "vault_doc_key")
        val metaSubkey = VaultCrypto.deriveSubkey(vmk, "vault_meta_key")
        val keys = ActiveVaultKeys(vmk, docSubkey, metaSubkey)

        val manifest = loadManifest(keys)
        updateFlows(manifest.documents)

        return keys
    }

    override fun observeDocuments(keys: ActiveVaultKeys): Flow<List<VaultDocument>> = documentsFlow.asStateFlow()

    override fun observeTrash(keys: ActiveVaultKeys): Flow<List<VaultDocument>> = trashFlow.asStateFlow()

    override fun getDocument(keys: ActiveVaultKeys, id: String): VaultDocument? {
        val manifest = loadManifest(keys)
        return manifest.documents.find { it.id == id }
    }

    override fun searchDocuments(keys: ActiveVaultKeys, query: String): List<VaultDocument> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val manifest = loadManifest(keys)
        return manifest.documents.filter { doc ->
            doc.trashedAtEpochMillis == null && doc.name.contains(q, ignoreCase = true)
        }
    }

    override fun createDocument(keys: ActiveVaultKeys, name: String, pages: List<NewPage>): VaultDocument {
        val safeName = name.trim()
        require(safeName.isNotEmpty()) { "Document name cannot be empty" }
        require(pages.isNotEmpty()) { "Document must have at least one page" }

        val docId = newId()
        val now = clock.millis()
        val vaultPages = mutableListOf<VaultPage>()

        val config = loadConfig() ?: throw VaultRepositoryError.NotConfigured()

        pages.forEachIndexed { position, page ->
            val pageId = newId()
            val origRel = saveEncryptedAsset(keys, config.vaultId, docId, pageId, AssetKind.ORIGINAL.name, page.original())
            val procRel = saveEncryptedAsset(keys, config.vaultId, docId, pageId, AssetKind.PROCESSED.name, page.processed())
            val thumbRel = saveEncryptedAsset(keys, config.vaultId, docId, pageId, AssetKind.THUMBNAIL.name, page.thumbnail())

            vaultPages.add(
                VaultPage(
                    id = pageId,
                    documentId = docId,
                    position = position,
                    originalAsset = origRel,
                    processedAsset = procRel,
                    thumbnailAsset = thumbRel,
                    width = page.width,
                    height = page.height,
                    cropCorners = page.cropCorners
                )
            )
        }

        val newDoc = VaultDocument(
            id = docId,
            name = safeName,
            createdAtEpochMillis = now,
            modifiedAtEpochMillis = now,
            pages = vaultPages
        )

        val manifest = loadManifest(keys)
        val updatedDocs = manifest.documents + newDoc
        saveManifest(keys, manifest.copy(documents = updatedDocs))
        updateFlows(updatedDocs)

        return newDoc
    }

    override fun renameDocument(keys: ActiveVaultKeys, id: String, name: String) {
        val safeName = name.trim()
        require(safeName.isNotEmpty()) { "Document name cannot be empty" }
        val manifest = loadManifest(keys)
        val doc = manifest.documents.find { it.id == id } ?: throw VaultRepositoryError.DocumentNotFound()
        val updatedDoc = doc.copy(name = safeName, modifiedAtEpochMillis = clock.millis())
        val updatedDocs = manifest.documents.map { if (it.id == id) updatedDoc else it }
        saveManifest(keys, manifest.copy(documents = updatedDocs))
        updateFlows(updatedDocs)
    }

    override fun setFavorite(keys: ActiveVaultKeys, id: String, favorite: Boolean) {
        val manifest = loadManifest(keys)
        val doc = manifest.documents.find { it.id == id } ?: throw VaultRepositoryError.DocumentNotFound()
        val updatedDoc = doc.copy(isFavorite = favorite, modifiedAtEpochMillis = clock.millis())
        val updatedDocs = manifest.documents.map { if (it.id == id) updatedDoc else it }
        saveManifest(keys, manifest.copy(documents = updatedDocs))
        updateFlows(updatedDocs)
    }

    override fun moveToTrash(keys: ActiveVaultKeys, id: String) {
        val manifest = loadManifest(keys)
        val doc = manifest.documents.find { it.id == id } ?: throw VaultRepositoryError.DocumentNotFound()
        val updatedDoc = doc.copy(trashedAtEpochMillis = clock.millis())
        val updatedDocs = manifest.documents.map { if (it.id == id) updatedDoc else it }
        saveManifest(keys, manifest.copy(documents = updatedDocs))
        updateFlows(updatedDocs)
    }

    override fun restoreFromTrash(keys: ActiveVaultKeys, id: String) {
        val manifest = loadManifest(keys)
        val doc = manifest.documents.find { it.id == id } ?: throw VaultRepositoryError.DocumentNotFound()
        val updatedDoc = doc.copy(trashedAtEpochMillis = null, modifiedAtEpochMillis = clock.millis())
        val updatedDocs = manifest.documents.map { if (it.id == id) updatedDoc else it }
        saveManifest(keys, manifest.copy(documents = updatedDocs))
        updateFlows(updatedDocs)
    }

    override fun permanentlyDelete(keys: ActiveVaultKeys, id: String) {
        val manifest = loadManifest(keys)
        val doc = manifest.documents.find { it.id == id } ?: throw VaultRepositoryError.DocumentNotFound()
        deleteDocumentAssets(doc)
        val updatedDocs = manifest.documents.filterNot { it.id == id }
        saveManifest(keys, manifest.copy(documents = updatedDocs))
        updateFlows(updatedDocs)
    }

    override fun emptyTrash(keys: ActiveVaultKeys) {
        val manifest = loadManifest(keys)
        val trashedDocs = manifest.documents.filter { it.trashedAtEpochMillis != null }
        trashedDocs.forEach { deleteDocumentAssets(it) }
        val updatedDocs = manifest.documents.filter { it.trashedAtEpochMillis == null }
        saveManifest(keys, manifest.copy(documents = updatedDocs))
        updateFlows(updatedDocs)
    }

    override fun moveDocumentIntoVault(
        keys: ActiveVaultKeys,
        doc: Document,
        fileStore: DocumentFileStore,
        repository: DocumentRepository
    ): VaultDocument {
        val transactionId = newId()
        journal.recordTransaction(
            TransactionRecord(transactionId, TransactionType.MOVE_IN, doc.id.value, TransactionStatus.STAGED_WRITE, clock.millis())
        )

        try {
            val newPages = doc.pages.map { page ->
                NewPage(
                    original = { fileStore.resolve(page.originalAsset).inputStream() },
                    processed = { fileStore.resolve(page.processedAsset).inputStream() },
                    thumbnail = { fileStore.resolve(page.thumbnailAsset).inputStream() },
                    width = page.width,
                    height = page.height,
                    cropCorners = page.cropCorners
                )
            }

            journal.recordTransaction(
                TransactionRecord(transactionId, TransactionType.MOVE_IN, doc.id.value, TransactionStatus.VERIFYING, clock.millis())
            )

            val vaultDoc = createDocument(keys, doc.name, newPages)

            journal.recordTransaction(
                TransactionRecord(transactionId, TransactionType.MOVE_IN, doc.id.value, TransactionStatus.COMMITTING, clock.millis())
            )

            // Delete standard library document
            kotlinx.coroutines.runBlocking {
                repository.permanentlyDelete(doc.id)
            }

            journal.clearTransaction(transactionId)
            return vaultDoc
        } catch (error: Throwable) {
            journal.clearTransaction(transactionId)
            throw VaultRepositoryError.StorageError("Failed to move document into Vault", error)
        }
    }

    override fun moveDocumentOutOfVault(
        keys: ActiveVaultKeys,
        vaultDocId: String,
        targetFolderId: FolderId?,
        fileStore: DocumentFileStore,
        repository: DocumentRepository
    ): DocumentId {
        val vaultDoc = getDocument(keys, vaultDocId) ?: throw VaultRepositoryError.DocumentNotFound()
        val transactionId = newId()
        journal.recordTransaction(
            TransactionRecord(transactionId, TransactionType.MOVE_OUT, vaultDocId, TransactionStatus.STAGED_WRITE, clock.millis())
        )

        try {
            val newPages = vaultDoc.pages.map { page ->
                NewPage(
                    original = { getDecryptedAssetStream(keys, vaultDoc.id, page.id, AssetKind.ORIGINAL.name, page.originalAsset) },
                    processed = { getDecryptedAssetStream(keys, vaultDoc.id, page.id, AssetKind.PROCESSED.name, page.processedAsset) },
                    thumbnail = { getDecryptedAssetStream(keys, vaultDoc.id, page.id, AssetKind.THUMBNAIL.name, page.thumbnailAsset) },
                    width = page.width,
                    height = page.height,
                    cropCorners = page.cropCorners
                )
            }

            val docId = kotlinx.coroutines.runBlocking {
                repository.create(vaultDoc.name, newPages, targetFolderId)
            }

            permanentlyDelete(keys, vaultDocId)
            journal.clearTransaction(transactionId)
            return docId
        } catch (error: Throwable) {
            journal.clearTransaction(transactionId)
            throw VaultRepositoryError.StorageError("Failed to move document out of Vault", error)
        }
    }

    override fun changePasscode(currentPasscode: String, newPasscode: String, keys: ActiveVaultKeys) {
        val validation = VaultCrypto.validatePasscode(newPasscode)
        if (validation is PasscodeValidationResult.Invalid) {
            throw VaultRepositoryError.InvalidPasscode(validation.reason)
        }

        val config = loadConfig() ?: throw VaultRepositoryError.NotConfigured()
        val kdfParams = KdfParameters(
            salt = config.saltHex.hexToByteArray(),
            iterations = config.iterations,
            memoryKb = config.memoryKb,
            parallelism = config.parallelism
        )
        val curPdk = VaultCrypto.derivePdk(currentPasscode, kdfParams)
        val wrapped = com.oscan.android.data.vault.crypto.WrappedVmkEnvelope(
            nonce = config.wrappedVmkNonceHex.hexToByteArray(),
            ciphertext = config.wrappedVmkCiphertextHex.hexToByteArray()
        )

        // Verify current passcode
        try {
            VaultCrypto.unwrapVmk(wrapped, curPdk)
        } catch (error: Throwable) {
            throw VaultRepositoryError.IncorrectPasscode(0)
        }

        // Re-wrap VMK with new passcode
        val newSalt = VaultCrypto.generateSalt()
        val newKdfParams = KdfParameters(salt = newSalt)
        val newPdk = VaultCrypto.derivePdk(newPasscode, newKdfParams)
        val newWrapped = VaultCrypto.wrapVmk(keys.vmk, newPdk)

        val newConfig = config.copy(
            saltHex = newSalt.toHex(),
            iterations = newKdfParams.iterations,
            memoryKb = newKdfParams.memoryKb,
            parallelism = newKdfParams.parallelism,
            wrappedVmkNonceHex = newWrapped.nonce.toHex(),
            wrappedVmkCiphertextHex = newWrapped.ciphertext.toHex(),
            failedAttempts = 0,
            nextAllowedAttemptEpochMillis = 0L
        )

        saveConfig(newConfig)
    }

    override fun resetVault() {
        if (vaultDir.exists()) {
            vaultDir.deleteRecursively()
        }
        vaultDir.mkdirs()
        assetsDir.mkdirs()
        cleanEphemeralCache()
        updateFlows(emptyList())
    }

    override fun disableVault(
        currentPasscode: String,
        keys: ActiveVaultKeys,
        migrateToLibrary: Boolean,
        fileStore: DocumentFileStore,
        repository: DocumentRepository
    ) {
        if (migrateToLibrary) {
            val manifest = loadManifest(keys)
            manifest.documents.forEach { vaultDoc ->
                moveDocumentOutOfVault(keys, vaultDoc.id, null, fileStore, repository)
            }
        }
        resetVault()
    }

    override fun cleanEphemeralCache() {
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
        cacheDir.mkdirs()
    }

    override fun getDecryptedAssetStream(
        keys: ActiveVaultKeys,
        documentId: String,
        pageId: String,
        assetKind: String,
        assetPath: String
    ): InputStream {
        val file = File(vaultDir, assetPath)
        check(file.exists()) { "Vault asset file does not exist: $assetPath" }
        val config = loadConfig() ?: throw VaultRepositoryError.NotConfigured()
        val aad = VaultCrypto.buildAad(config.vaultId, documentId, pageId, assetKind)
        val decrypted = VaultCrypto.decryptStream(keys.docSubkey, aad, file.inputStream())
        return ByteArrayInputStream(decrypted)
    }

    private fun saveEncryptedAsset(
        keys: ActiveVaultKeys,
        vaultId: String,
        documentId: String,
        pageId: String,
        assetKind: String,
        inputStream: InputStream
    ): String {
        val fileName = "${documentId}_${pageId}_${assetKind.lowercase()}.enc"
        val file = File(assetsDir, fileName)
        val aad = VaultCrypto.buildAad(vaultId, documentId, pageId, assetKind)
        file.outputStream().use { out ->
            VaultCrypto.encryptStream(keys.docSubkey, aad, inputStream, out)
        }
        return "assets/$fileName"
    }

    private fun deleteDocumentAssets(doc: VaultDocument) {
        doc.pages.forEach { page ->
            File(vaultDir, page.originalAsset).takeIf { it.exists() }?.delete()
            File(vaultDir, page.processedAsset).takeIf { it.exists() }?.delete()
            File(vaultDir, page.thumbnailAsset).takeIf { it.exists() }?.delete()
        }
    }

    private fun loadManifest(keys: ActiveVaultKeys): VaultManifest {
        if (!manifestFile.exists()) return VaultManifest(emptyList())
        val config = loadConfig() ?: throw VaultRepositoryError.NotConfigured()
        val aad = VaultCrypto.buildAad(config.vaultId, "manifest", "manifest", "MANIFEST")
        return try {
            val decryptedBytes = VaultCrypto.decryptPayload(keys.metaSubkey, aad, manifestFile.readBytes())
            val json = String(decryptedBytes, Charsets.UTF_8)
            parseManifest(json)
        } catch (error: Throwable) {
            VaultManifest(emptyList())
        }
    }

    private fun saveManifest(keys: ActiveVaultKeys, manifest: VaultManifest) {
        val config = loadConfig() ?: throw VaultRepositoryError.NotConfigured()
        val json = serializeManifest(manifest)
        val aad = VaultCrypto.buildAad(config.vaultId, "manifest", "manifest", "MANIFEST")
        val encrypted = VaultCrypto.encryptPayload(keys.metaSubkey, aad, json.toByteArray(Charsets.UTF_8))
        val tempFile = File(vaultDir, "manifest.enc.tmp")
        tempFile.writeBytes(encrypted)
        if (manifestFile.exists()) {
            manifestFile.delete()
        }
        tempFile.renameTo(manifestFile)
    }

    private fun updateFlows(documents: List<VaultDocument>) {
        documentsFlow.value = documents.filter { it.trashedAtEpochMillis == null }
        trashFlow.value = documents.filter { it.trashedAtEpochMillis != null }
    }

    private fun calculateLockoutDelaySeconds(failures: Int): Int {
        return when {
            failures < 5 -> 0
            failures == 5 -> 30
            failures == 6 -> 60
            failures == 7 -> 120
            failures == 8 -> 300
            failures == 9 -> 600
            else -> 3600
        }
    }

    private fun loadConfig(): VaultConfig? {
        if (!configFile.exists()) return null
        return runCatching {
            val text = configFile.readText(Charsets.UTF_8)
            parseConfig(text)
        }.getOrNull()
    }

    private fun saveConfig(config: VaultConfig) {
        val text = serializeConfig(config)
        val tempFile = File(vaultDir, "vault_config.json.tmp")
        tempFile.writeText(text, Charsets.UTF_8)
        if (configFile.exists()) {
            configFile.delete()
        }
        tempFile.renameTo(configFile)
    }

    private fun serializeConfig(config: VaultConfig): String {
        return "version=${config.version}\n" +
                "vaultId=${config.vaultId}\n" +
                "kdfAlgorithm=${config.kdfAlgorithm}\n" +
                "kdfVersion=${config.kdfVersion}\n" +
                "saltHex=${config.saltHex}\n" +
                "iterations=${config.iterations}\n" +
                "memoryKb=${config.memoryKb}\n" +
                "parallelism=${config.parallelism}\n" +
                "wrappedVmkNonceHex=${config.wrappedVmkNonceHex}\n" +
                "wrappedVmkCiphertextHex=${config.wrappedVmkCiphertextHex}\n" +
                "failedAttempts=${config.failedAttempts}\n" +
                "nextAllowedAttemptEpochMillis=${config.nextAllowedAttemptEpochMillis}"
    }

    private fun parseConfig(text: String): VaultConfig {
        val lines = text.lines().associate { line ->
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to ""
        }
        return VaultConfig(
            version = lines["version"]?.toIntOrNull() ?: 1,
            vaultId = lines["vaultId"] ?: "",
            kdfAlgorithm = lines["kdfAlgorithm"] ?: "Argon2id",
            kdfVersion = lines["kdfVersion"]?.toIntOrNull() ?: 1,
            saltHex = lines["saltHex"] ?: "",
            iterations = lines["iterations"]?.toIntOrNull() ?: 3,
            memoryKb = lines["memoryKb"]?.toIntOrNull() ?: 65536,
            parallelism = lines["parallelism"]?.toIntOrNull() ?: 1,
            wrappedVmkNonceHex = lines["wrappedVmkNonceHex"] ?: "",
            wrappedVmkCiphertextHex = lines["wrappedVmkCiphertextHex"] ?: "",
            failedAttempts = lines["failedAttempts"]?.toIntOrNull() ?: 0,
            nextAllowedAttemptEpochMillis = lines["nextAllowedAttemptEpochMillis"]?.toLongOrNull() ?: 0L
        )
    }

    private fun serializeManifest(manifest: VaultManifest): String {
        return manifest.documents.joinToString("\n---DOC---\n") { doc ->
            val pagesStr = doc.pages.joinToString(";") { p ->
                val cropStr = p.cropCorners?.let { c ->
                    c.toArray().joinToString(",") { "${it.x}:${it.y}" }
                } ?: ""
                "${p.id}|${p.position}|${p.originalAsset}|${p.processedAsset}|${p.thumbnailAsset}|${p.width}|${p.height}|${p.rotationDegrees}|$cropStr"
            }
            "id=${doc.id}\nname=${doc.name}\nisFavorite=${doc.isFavorite}\ncreatedAtEpochMillis=${doc.createdAtEpochMillis}\nmodifiedAtEpochMillis=${doc.modifiedAtEpochMillis}\ntrashedAtEpochMillis=${doc.trashedAtEpochMillis ?: ""}\npages=$pagesStr"
        }
    }

    private fun parseManifest(text: String): VaultManifest {
        if (text.isBlank()) return VaultManifest(emptyList())
        val docs = mutableListOf<VaultDocument>()
        val blocks = text.split("---DOC---").filter { it.isNotBlank() }
        for (block in blocks) {
            val lines = block.lines().associate { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to ""
            }
            val id = lines["id"] ?: continue
            val name = lines["name"] ?: ""
            val isFavorite = lines["isFavorite"]?.toBooleanStrictOrNull() ?: false
            val createdAt = lines["createdAtEpochMillis"]?.toLongOrNull() ?: 0L
            val modifiedAt = lines["modifiedAtEpochMillis"]?.toLongOrNull() ?: 0L
            val trashedAt = lines["trashedAtEpochMillis"]?.takeIf { it.isNotBlank() }?.toLongOrNull()

            val pagesStr = lines["pages"] ?: ""
            val pages = if (pagesStr.isNotBlank()) {
                pagesStr.split(";").mapNotNull { pStr ->
                    val tokens = pStr.split("|")
                    if (tokens.size >= 8) {
                        val cropPoints = if (tokens.size >= 9 && tokens[8].isNotBlank()) {
                            runCatching {
                                val pts = tokens[8].split(",").map { encoded ->
                                    val coords = encoded.split(":")
                                    org.opencv.core.Point(coords[0].toDouble(), coords[1].toDouble())
                                }
                                CornerPoints.fromArray(pts.toTypedArray())
                            }.getOrNull()
                        } else null

                        VaultPage(
                            id = tokens[0],
                            documentId = id,
                            position = tokens[1].toIntOrNull() ?: 0,
                            originalAsset = tokens[2],
                            processedAsset = tokens[3],
                            thumbnailAsset = tokens[4],
                            width = tokens[5].toIntOrNull() ?: 0,
                            height = tokens[6].toIntOrNull() ?: 0,
                            rotationDegrees = tokens[7].toIntOrNull() ?: 0,
                            cropCorners = cropPoints
                        )
                    } else null
                }
            } else emptyList()

            docs.add(
                VaultDocument(
                    id = id,
                    name = name,
                    isFavorite = isFavorite,
                    createdAtEpochMillis = createdAt,
                    modifiedAtEpochMillis = modifiedAt,
                    trashedAtEpochMillis = trashedAt,
                    pages = pages
                )
            )
        }
        return VaultManifest(docs)
    }
}
