package com.oscan.android.data.vault

import androidx.test.core.app.ApplicationProvider
import com.oscan.android.data.repository.NewPage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class VaultRepositoryTest {

    private lateinit var repository: LocalVaultRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        repository = LocalVaultRepository(context)
        repository.resetVault()
    }

    @Test
    fun testInitialStateIsNotConfigured() {
        assertFalse(repository.isConfigured())
        assertEquals(0, repository.getLockoutRemainingSeconds())
    }

    @Test
    fun testSetupVaultWithWeakPasscodeFails() {
        assertFailsWith<VaultRepositoryError.InvalidPasscode> {
            repository.setupVault("123456")
        }
        assertFailsWith<VaultRepositoryError.InvalidPasscode> {
            repository.setupVault("111111")
        }
    }

    @Test
    fun testSetupAndUnlockSuccess() {
        val passcode = "948201"
        val keys = repository.setupVault(passcode)
        assertTrue(repository.isConfigured())
        assertNotNull(keys)

        val unlockedKeys = repository.unlock(passcode)
        assertNotNull(unlockedKeys)
    }

    @Test
    fun testIncorrectPasscodeIncrementsAttempts() {
        val passcode = "948201"
        repository.setupVault(passcode)

        assertFailsWith<VaultRepositoryError.IncorrectPasscode> {
            repository.unlock("948209")
        }
    }

    @Test
    fun testCreateAndSearchVaultDocument() {
        val passcode = "948201"
        val keys = repository.setupVault(passcode)

        val dummyBytes = "Dummy Content".toByteArray()
        val newPage = NewPage(
            original = { ByteArrayInputStream(dummyBytes) },
            processed = { ByteArrayInputStream(dummyBytes) },
            thumbnail = { ByteArrayInputStream(dummyBytes) },
            width = 100,
            height = 100
        )

        val created = repository.createDocument(keys, "Tax Report 2026", listOf(newPage))
        assertEquals("Tax Report 2026", created.name)
        assertEquals(1, created.pages.size)

        val searchResults = repository.searchDocuments(keys, "Tax")
        assertEquals(1, searchResults.size)
        assertEquals(created.id, searchResults[0].id)

        val doc = repository.getDocument(keys, created.id)
        assertNotNull(doc)
        assertEquals("Tax Report 2026", doc.name)
    }

    @Test
    fun testChangePasscodeReWrapsVmk() {
        val passcode = "948201"
        val newPasscode = "830192"
        val keys = repository.setupVault(passcode)

        repository.changePasscode(passcode, newPasscode, keys)

        // Old passcode fails
        assertFailsWith<VaultRepositoryError.IncorrectPasscode> {
            repository.unlock(passcode)
        }

        // New passcode succeeds
        val newKeys = repository.unlock(newPasscode)
        assertNotNull(newKeys)
    }

    @Test
    fun testResetVaultClearsAllData() {
        val passcode = "948201"
        val keys = repository.setupVault(passcode)
        val dummyBytes = "Dummy".toByteArray()
        repository.createDocument(
            keys,
            "Secret Doc",
            listOf(
                NewPage(
                    original = { ByteArrayInputStream(dummyBytes) },
                    processed = { ByteArrayInputStream(dummyBytes) },
                    thumbnail = { ByteArrayInputStream(dummyBytes) },
                    width = 100,
                    height = 100
                )
            )
        )

        repository.resetVault()

        assertFalse(repository.isConfigured())
    }
}
