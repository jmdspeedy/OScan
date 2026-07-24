package com.oscan.android.data.vault.crypto

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VaultCryptoTest {

    @Test
    fun testPasscodeValidation() {
        // Valid passcodes
        assertTrue(VaultCrypto.validatePasscode("184920") is PasscodeValidationResult.Valid)
        assertTrue(VaultCrypto.validatePasscode("94810293") is PasscodeValidationResult.Valid)

        // Too short / too long
        assertTrue(VaultCrypto.validatePasscode("12345") is PasscodeValidationResult.Invalid)
        assertTrue(VaultCrypto.validatePasscode("1234567890123") is PasscodeValidationResult.Invalid)

        // Non-digit
        assertTrue(VaultCrypto.validatePasscode("12345a") is PasscodeValidationResult.Invalid)

        // Single repeated digit
        assertTrue(VaultCrypto.validatePasscode("111111") is PasscodeValidationResult.Invalid)
        assertTrue(VaultCrypto.validatePasscode("99999999") is PasscodeValidationResult.Invalid)

        // Sequential
        assertTrue(VaultCrypto.validatePasscode("123456") is PasscodeValidationResult.Invalid)
        assertTrue(VaultCrypto.validatePasscode("654321") is PasscodeValidationResult.Invalid)
    }

    @Test
    fun testKdfAndVmkWrappingRoundTrip() {
        val passcode = "948201"
        val salt = VaultCrypto.generateSalt()
        val kdfParams = KdfParameters(salt = salt, iterations = 2, memoryKb = 16384, parallelism = 1)

        val pdk = VaultCrypto.derivePdk(passcode, kdfParams)
        val vmk = VaultCrypto.generateVmk()

        val wrapped = VaultCrypto.wrapVmk(vmk, pdk)
        val unwrappedVmk = VaultCrypto.unwrapVmk(wrapped, pdk)

        assertTrue(vmk.contentEquals(unwrappedVmk))
    }

    @Test
    fun testUnwrapVmkWithWrongPasscodeFails() {
        val passcode = "948201"
        val wrongPasscode = "948202"
        val salt = VaultCrypto.generateSalt()
        val kdfParams = KdfParameters(salt = salt, iterations = 2, memoryKb = 16384, parallelism = 1)

        val pdk = VaultCrypto.derivePdk(passcode, kdfParams)
        val wrongPdk = VaultCrypto.derivePdk(wrongPasscode, kdfParams)
        val vmk = VaultCrypto.generateVmk()

        val wrapped = VaultCrypto.wrapVmk(vmk, pdk)

        assertFailsWith<VaultCryptoException.AuthenticationFailed> {
            VaultCrypto.unwrapVmk(wrapped, wrongPdk)
        }
    }

    @Test
    fun testPayloadEncryptionAndAadBinding() {
        val vmk = VaultCrypto.generateVmk()
        val docSubkey = VaultCrypto.deriveSubkey(vmk, "vault_doc_key")
        val aad = VaultCrypto.buildAad("vault_1", "doc_1", "page_1", "ORIGINAL")
        val wrongAad = VaultCrypto.buildAad("vault_1", "doc_1", "page_2", "ORIGINAL")
        val plaintext = "Sensitive Document Byte Data".toByteArray(Charsets.UTF_8)

        val envelope = VaultCrypto.encryptPayload(docSubkey, aad, plaintext)
        val decrypted = VaultCrypto.decryptPayload(docSubkey, aad, envelope)

        assertEquals("Sensitive Document Byte Data", String(decrypted, Charsets.UTF_8))

        // Mismatched AAD fails authentication
        assertFailsWith<VaultCryptoException.IntegrityError> {
            VaultCrypto.decryptPayload(docSubkey, wrongAad, envelope)
        }

        // Tampered ciphertext fails
        envelope[envelope.size - 1] = (envelope[envelope.size - 1].toInt() xor 0xFF).toByte()
        assertFailsWith<VaultCryptoException.IntegrityError> {
            VaultCrypto.decryptPayload(docSubkey, aad, envelope)
        }
    }
}
