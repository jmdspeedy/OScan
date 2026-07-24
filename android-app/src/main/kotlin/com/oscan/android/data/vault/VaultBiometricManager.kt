package com.oscan.android.data.vault

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class VaultBiometricManager(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isAvailable(): Boolean =
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS

    fun isSupported(): Boolean =
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE

    fun isEnabled(): Boolean =
        preferences.contains(KEY_CIPHERTEXT) &&
            preferences.contains(KEY_IV) &&
            keyStore().containsAlias(KEY_ALIAS)

    fun createEnrollmentCipher(): Cipher {
        check(isAvailable()) { "Strong biometric authentication is not available on this device" }
        disable()
        val key = generateKey()
        return newCipher().apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
    }

    fun completeEnrollment(cipher: Cipher, vmk: ByteArray) {
        val ciphertext = cipher.doFinal(vmk)
        preferences.edit()
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .apply()
    }

    fun createUnlockCipher(): Cipher {
        check(isEnabled()) { "Fingerprint unlock is not enabled" }
        val iv = preferences.getString(KEY_IV, null)?.decodeBase64()
            ?: error("Fingerprint unlock data is missing")
        return newCipher().apply {
            init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        }
    }

    fun completeUnlock(cipher: Cipher): ByteArray {
        val ciphertext = preferences.getString(KEY_CIPHERTEXT, null)?.decodeBase64()
            ?: error("Fingerprint unlock data is missing")
        return try {
            cipher.doFinal(ciphertext)
        } catch (error: Throwable) {
            disable()
            throw error
        }
    }

    fun disable() {
        preferences.edit().clear().apply()
        val store = keyStore()
        if (store.containsAlias(KEY_ALIAS)) {
            store.deleteEntry(KEY_ALIAS)
        }
    }

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        )
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }

        generator.init(builder.build())
        return generator.generateKey()
    }

    private fun getKey(): SecretKey =
        keyStore().getKey(KEY_ALIAS, null) as? SecretKey
            ?: error("Fingerprint unlock key is unavailable")

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    private fun newCipher(): Cipher =
        Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}")

    private fun String.decodeBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "oscan_vault_biometric_v1"
        const val PREFERENCES_NAME = "oscan_vault_biometric"
        const val KEY_IV = "iv"
        const val KEY_CIPHERTEXT = "ciphertext"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
