package com.oscan.android.data.vault

import com.oscan.android.data.vault.crypto.VaultCrypto
import com.oscan.core.model.CornerPoints

data class VaultPage(
    val id: String,
    val documentId: String,
    val position: Int,
    val originalAsset: String,
    val processedAsset: String,
    val thumbnailAsset: String,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int = 0,
    val cropCorners: CornerPoints? = null
)

data class VaultDocument(
    val id: String,
    val name: String,
    val isFavorite: Boolean = false,
    val createdAtEpochMillis: Long,
    val modifiedAtEpochMillis: Long,
    val trashedAtEpochMillis: Long? = null,
    val pages: List<VaultPage> = emptyList()
)

data class VaultManifest(
    val documents: List<VaultDocument> = emptyList()
)

data class VaultConfig(
    val version: Int = 1,
    val vaultId: String,
    val kdfAlgorithm: String = "Argon2id",
    val kdfVersion: Int = 1,
    val saltHex: String,
    val iterations: Int = VaultCrypto.CURRENT_KDF_ITERATIONS,
    val memoryKb: Int = VaultCrypto.CURRENT_KDF_MEMORY_KB,
    val parallelism: Int = VaultCrypto.CURRENT_KDF_PARALLELISM,
    val wrappedVmkNonceHex: String,
    val wrappedVmkCiphertextHex: String,
    val failedAttempts: Int = 0,
    val nextAllowedAttemptEpochMillis: Long = 0L
)

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

fun String.hexToByteArray(): ByteArray {
    check(length % 2 == 0) { "Hex string length must be even" }
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
