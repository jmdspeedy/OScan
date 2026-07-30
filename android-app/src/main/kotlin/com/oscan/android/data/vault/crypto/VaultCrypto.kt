package com.oscan.android.data.vault.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

sealed class VaultCryptoException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidPasscode(message: String) : VaultCryptoException(message)
    class AuthenticationFailed(cause: Throwable? = null) : VaultCryptoException("Passcode authentication failed", cause)
    class IntegrityError(message: String, cause: Throwable? = null) : VaultCryptoException(message, cause)
    class UnsupportedVersion(version: Int) : VaultCryptoException("Unsupported envelope version: $version")
}

sealed class PasscodeValidationResult {
    object Valid : PasscodeValidationResult()
    data class Invalid(val reason: String) : PasscodeValidationResult()
}

data class KdfParameters(
    val algorithm: String = "Argon2id",
    val version: Int = 1,
    val salt: ByteArray,
    val iterations: Int = VaultCrypto.CURRENT_KDF_ITERATIONS,
    val memoryKb: Int = VaultCrypto.CURRENT_KDF_MEMORY_KB,
    val parallelism: Int = 1
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KdfParameters
        return algorithm == other.algorithm &&
                version == other.version &&
                salt.contentEquals(other.salt) &&
                iterations == other.iterations &&
                memoryKb == other.memoryKb &&
                parallelism == other.parallelism
    }

    override fun hashCode(): Int {
        var result = algorithm.hashCode()
        result = 31 * result + version
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + iterations
        result = 31 * result + memoryKb
        result = 31 * result + parallelism
        return result
    }
}

data class WrappedVmkEnvelope(
    val nonce: ByteArray,
    val ciphertext: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WrappedVmkEnvelope
        return nonce.contentEquals(other.nonce) && ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int {
        var result = nonce.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }
}

object VaultCrypto {
    // OWASP's mobile-friendly Argon2id baseline. The previous 64 MiB / 3-pass
    // profile took several seconds in Bouncy Castle on typical Android devices.
    const val CURRENT_KDF_ITERATIONS = 2
    const val CURRENT_KDF_MEMORY_KB = 19 * 1024
    const val CURRENT_KDF_PARALLELISM = 1

    private const val GCM_NONCE_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val KEY_LENGTH_BYTES = 32
    private const val SALT_LENGTH_BYTES = 16
    private const val ENVELOPE_VERSION: Byte = 0x01
    private val VMK_AAD = "vault_vmk_v1".toByteArray(Charsets.UTF_8)

    private val secureRandom = SecureRandom()

    fun validatePasscode(passcode: String): PasscodeValidationResult {
        if (!passcode.matches(Regex("^\\d{6}$"))) {
            return PasscodeValidationResult.Invalid("Passcode must be exactly 6 digits")
        }
        val firstChar = passcode[0]
        if (passcode.all { it == firstChar }) {
            return PasscodeValidationResult.Invalid("Passcode cannot be a single repeating digit")
        }
        val isAscending = passcode.indices.all { i -> i == 0 || passcode[i] == passcode[i - 1] + 1 }
        val isDescending = passcode.indices.all { i -> i == 0 || passcode[i] == passcode[i - 1] - 1 }
        if (isAscending || isDescending) {
            return PasscodeValidationResult.Invalid("Passcode cannot be a sequential sequence")
        }
        return PasscodeValidationResult.Valid
    }

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun generateVmk(): ByteArray {
        val vmk = ByteArray(KEY_LENGTH_BYTES)
        secureRandom.nextBytes(vmk)
        return vmk
    }

    fun derivePdk(passcode: String, kdfParams: KdfParameters): ByteArray {
        val passcodeBytes = passcode.toByteArray(Charsets.UTF_8)
        val pdk = ByteArray(KEY_LENGTH_BYTES)
        try {
            val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(kdfParams.iterations)
                .withMemoryAsKB(kdfParams.memoryKb)
                .withParallelism(kdfParams.parallelism)
                .withSalt(kdfParams.salt)

            val generator = Argon2BytesGenerator()
            generator.init(builder.build())
            generator.generateBytes(passcodeBytes, pdk, 0, pdk.size)
            return pdk
        } finally {
            wipe(passcodeBytes)
        }
    }

    fun wrapVmk(vmk: ByteArray, pdk: ByteArray): WrappedVmkEnvelope {
        val nonce = ByteArray(GCM_NONCE_LENGTH)
        secureRandom.nextBytes(nonce)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(pdk, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        cipher.updateAAD(VMK_AAD)

        val ciphertext = cipher.doFinal(vmk)
        return WrappedVmkEnvelope(nonce, ciphertext)
    }

    fun unwrapVmk(wrapped: WrappedVmkEnvelope, pdk: ByteArray): ByteArray {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(pdk, "AES")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, wrapped.nonce)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            cipher.updateAAD(VMK_AAD)

            return cipher.doFinal(wrapped.ciphertext)
        } catch (error: Throwable) {
            throw VaultCryptoException.AuthenticationFailed(error)
        }
    }

    fun deriveSubkey(vmk: ByteArray, domain: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(vmk, "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(domain.toByteArray(Charsets.UTF_8))
    }

    fun encryptPayload(key: ByteArray, aad: ByteArray, plaintext: ByteArray): ByteArray {
        val nonce = ByteArray(GCM_NONCE_LENGTH)
        secureRandom.nextBytes(nonce)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        if (aad.isNotEmpty()) {
            cipher.updateAAD(aad)
        }

        val ciphertext = cipher.doFinal(plaintext)
        val envelope = ByteArray(1 + GCM_NONCE_LENGTH + ciphertext.size)
        envelope[0] = ENVELOPE_VERSION
        System.arraycopy(nonce, 0, envelope, 1, GCM_NONCE_LENGTH)
        System.arraycopy(ciphertext, 0, envelope, 1 + GCM_NONCE_LENGTH, ciphertext.size)
        return envelope
    }

    fun decryptPayload(key: ByteArray, aad: ByteArray, envelope: ByteArray): ByteArray {
        if (envelope.size < 1 + GCM_NONCE_LENGTH + 16) {
            throw VaultCryptoException.IntegrityError("Ciphertext payload is truncated")
        }
        val version = envelope[0]
        if (version != ENVELOPE_VERSION) {
            throw VaultCryptoException.UnsupportedVersion(version.toInt())
        }

        val nonce = ByteArray(GCM_NONCE_LENGTH)
        System.arraycopy(envelope, 1, nonce, 0, GCM_NONCE_LENGTH)

        val ciphertextLength = envelope.size - 1 - GCM_NONCE_LENGTH
        val ciphertext = ByteArray(ciphertextLength)
        System.arraycopy(envelope, 1 + GCM_NONCE_LENGTH, ciphertext, 0, ciphertextLength)

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(key, "AES")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            if (aad.isNotEmpty()) {
                cipher.updateAAD(aad)
            }
            return cipher.doFinal(ciphertext)
        } catch (error: Throwable) {
            throw VaultCryptoException.IntegrityError("Payload decryption or authentication failed", error)
        }
    }

    fun encryptStream(key: ByteArray, aad: ByteArray, inputStream: InputStream, outputStream: OutputStream) {
        val plaintext = inputStream.readBytes()
        val encrypted = encryptPayload(key, aad, plaintext)
        outputStream.write(encrypted)
        outputStream.flush()
    }

    fun decryptStream(key: ByteArray, aad: ByteArray, inputStream: InputStream): ByteArray {
        val encrypted = inputStream.readBytes()
        return decryptPayload(key, aad, encrypted)
    }

    fun buildAad(vaultId: String, documentId: String, pageId: String, assetKind: String, version: String = "1"): ByteArray {
        return "$vaultId:$documentId:$pageId:$assetKind:$version".toByteArray(Charsets.UTF_8)
    }

    fun wipe(array: ByteArray) {
        Arrays.fill(array, 0.toByte())
    }
}
