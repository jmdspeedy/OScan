package com.oscan.android.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscan.android.data.model.Document
import com.oscan.android.data.model.FolderId
import com.oscan.android.data.repository.DocumentRepository
import com.oscan.android.data.storage.DocumentFileStore
import com.oscan.android.data.vault.ActiveVaultKeys
import com.oscan.android.data.vault.VaultBiometricManager
import com.oscan.android.data.vault.VaultDocument
import com.oscan.android.data.vault.VaultRepository
import com.oscan.android.data.vault.VaultRepositoryError
import com.oscan.android.data.vault.VaultSessionManager
import com.oscan.android.data.vault.VaultSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.Cipher

data class VaultUiState(
    val isConfigured: Boolean = false,
    val sessionState: VaultSessionState = VaultSessionState.Locked(),
    val documents: List<VaultDocument> = emptyList(),
    val trashDocuments: List<VaultDocument> = emptyList(),
    val selectedDocumentId: String? = null,
    val selectedDocument: VaultDocument? = null,
    val searchQuery: String = "",
    val searchResults: List<VaultDocument> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val isViewingTrash: Boolean = false,
    val isExporting: Boolean = false,
    val isMovingDocument: Boolean = false,
    val biometricSupported: Boolean = false,
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = false
)

class VaultViewModel(
    val vaultRepository: VaultRepository,
    val sessionManager: VaultSessionManager,
    private val biometricManager: VaultBiometricManager,
    val fileStore: DocumentFileStore,
    val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()
    private var lockoutCountdownJob: Job? = null

    init {
        refreshConfiguredState()

        sessionManager.sessionState.onEach { session ->
            _uiState.value = _uiState.value.copy(sessionState = session)
            val keys = sessionManager.getActiveKeys()
            if (keys != null) {
                observeVaultContent(keys)
            } else {
                _uiState.value = _uiState.value.copy(
                    documents = emptyList(),
                    trashDocuments = emptyList(),
                    selectedDocumentId = null,
                    selectedDocument = null,
                    searchQuery = "",
                    searchResults = emptyList(),
                    selectionMode = false,
                    selectedIds = emptySet()
                )
            }
        }.launchIn(viewModelScope)
    }

    fun refreshConfiguredState() {
        val configured = vaultRepository.isConfigured()
        if (!configured) {
            lockoutCountdownJob?.cancel()
            sessionManager.setNotConfigured()
            biometricManager.disable()
        } else if (!sessionManager.isUnlocked()) {
            updateLockoutState()
        }
        _uiState.value = _uiState.value.copy(
            isConfigured = configured,
            biometricSupported = biometricManager.isSupported(),
            biometricAvailable = biometricManager.isAvailable(),
            biometricEnabled = configured && biometricManager.isEnabled()
        )
    }

    private fun updateLockoutState() {
        val remaining = vaultRepository.getLockoutRemainingSeconds()
        sessionManager.setLocked(remaining)
        if (remaining > 0) {
            startLockoutCountdown()
        }
    }

    private fun startLockoutCountdown() {
        lockoutCountdownJob?.cancel()
        lockoutCountdownJob = viewModelScope.launch {
            while (true) {
                val remaining = vaultRepository.getLockoutRemainingSeconds()
                sessionManager.setLocked(remaining)
                if (remaining <= 0) break
                delay(1_000)
            }
        }
    }

    private fun observeVaultContent(keys: ActiveVaultKeys) {
        vaultRepository.observeDocuments(keys).onEach { docs ->
            val selId = _uiState.value.selectedDocumentId
            val selDoc = docs.find { it.id == selId }
            _uiState.value = _uiState.value.copy(documents = docs, selectedDocument = selDoc)
        }.launchIn(viewModelScope)

        vaultRepository.observeTrash(keys).onEach { trash ->
            _uiState.value = _uiState.value.copy(trashDocuments = trash)
        }.launchIn(viewModelScope)
    }

    fun setupVault(passcode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(errorMessage = null)
            try {
                val keys = withContext(Dispatchers.IO) {
                    vaultRepository.setupVault(passcode)
                }
                refreshConfiguredState()
                lockoutCountdownJob?.cancel()
                sessionManager.unlockWithVmk(keys.vmk)
                _uiState.value = _uiState.value.copy(
                    errorMessage = null,
                    infoMessage = "Vault configured successfully"
                )
            } catch (error: VaultRepositoryError.InvalidPasscode) {
                _uiState.value = _uiState.value.copy(errorMessage = error.reason)
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to setup Vault: ${error.message}")
            }
        }
    }

    fun unlockVault(passcode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(errorMessage = null)
            try {
                val keys = withContext(Dispatchers.IO) {
                    vaultRepository.unlock(passcode)
                }
                lockoutCountdownJob?.cancel()
                sessionManager.unlockWithVmk(keys.vmk)
                _uiState.value = _uiState.value.copy(errorMessage = null)
            } catch (error: VaultRepositoryError.LockedOut) {
                _uiState.value = _uiState.value.copy(errorMessage = error.message)
                sessionManager.setLocked(error.remainingSeconds)
                startLockoutCountdown()
            } catch (error: VaultRepositoryError.IncorrectPasscode) {
                _uiState.value = _uiState.value.copy(errorMessage = "Incorrect passcode")
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(errorMessage = "Unlock failed: ${error.message}")
            }
        }
    }

    fun lockVault() {
        sessionManager.lock()
        _uiState.value = _uiState.value.copy(
            selectedDocumentId = null,
            selectedDocument = null,
            searchQuery = "",
            searchResults = emptyList(),
            selectionMode = false,
            selectedIds = emptySet(),
            isViewingTrash = false
        )
    }

    fun resetVault() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                vaultRepository.resetVault()
                biometricManager.disable()
            }
            refreshConfiguredState()
            sessionManager.setNotConfigured()
            _uiState.value = _uiState.value.copy(
                infoMessage = "Vault reset completed",
                selectedDocumentId = null,
                selectedDocument = null,
                isViewingTrash = false
            )
        }
    }

    fun changePasscode(currentPasscode: String, newPasscode: String) {
        val keys = sessionManager.getActiveKeys() ?: return
        viewModelScope.launch {
            try {
                vaultRepository.changePasscode(currentPasscode, newPasscode, keys)
                _uiState.value = _uiState.value.copy(infoMessage = "Vault passcode updated")
            } catch (error: VaultRepositoryError.InvalidPasscode) {
                _uiState.value = _uiState.value.copy(errorMessage = error.reason)
            } catch (error: VaultRepositoryError.IncorrectPasscode) {
                _uiState.value = _uiState.value.copy(errorMessage = "Current passcode is incorrect")
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to change passcode: ${error.message}")
            }
        }
    }

    fun disableVault(currentPasscode: String, migrateToLibrary: Boolean) {
        val keys = sessionManager.getActiveKeys() ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    vaultRepository.disableVault(currentPasscode, keys, migrateToLibrary, fileStore, documentRepository)
                    biometricManager.disable()
                }
                refreshConfiguredState()
                sessionManager.setNotConfigured()
                _uiState.value = _uiState.value.copy(infoMessage = "Vault disabled")
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to disable Vault: ${error.message}")
            }
        }
    }

    fun moveDocumentIntoVault(doc: Document) {
        val keys = sessionManager.getActiveKeys() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isMovingDocument = true,
                errorMessage = null,
                infoMessage = null
            )
            try {
                withContext(Dispatchers.IO) {
                    vaultRepository.moveDocumentIntoVault(keys, doc, fileStore, documentRepository)
                }
                _uiState.value = _uiState.value.copy(
                    isMovingDocument = false,
                    infoMessage = "Moved \"${doc.name}\" to Vault"
                )
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isMovingDocument = false,
                    errorMessage = "Failed to move to Vault: ${error.message}"
                )
            }
        }
    }

    /**
     * Moves a list of documents into the Vault.
     *
     * @param docs The list of documents to move into the Vault.
     */
    fun moveDocumentsIntoVault(docs: List<Document>) {
        if (docs.isEmpty()) return
        if (docs.size == 1) {
            moveDocumentIntoVault(docs.first())
            return
        }
        val keys = sessionManager.getActiveKeys() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isMovingDocument = true,
                errorMessage = null,
                infoMessage = null
            )
            try {
                var successCount = 0
                withContext(Dispatchers.IO) {
                    docs.forEach { doc ->
                        vaultRepository.moveDocumentIntoVault(keys, doc, fileStore, documentRepository)
                        successCount++
                    }
                }
                _uiState.value = _uiState.value.copy(
                    isMovingDocument = false,
                    infoMessage = "Moved $successCount documents to Vault"
                )
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isMovingDocument = false,
                    errorMessage = "Failed to move to Vault: ${error.message}"
                )
            }
        }
    }

    fun moveDocumentOutOfVault(vaultDocId: String, targetFolderId: FolderId?) {
        val keys = sessionManager.getActiveKeys() ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    vaultRepository.moveDocumentOutOfVault(keys, vaultDocId, targetFolderId, fileStore, documentRepository)
                }
                _uiState.value = _uiState.value.copy(
                    infoMessage = "Moved document out of Vault to standard library",
                    selectedDocumentId = null,
                    selectedDocument = null
                )
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to move out of Vault: ${error.message}")
            }
        }
    }

    fun renameDocument(id: String, name: String) {
        val keys = sessionManager.getActiveKeys() ?: return
        viewModelScope.launch {
            try {
                vaultRepository.renameDocument(keys, id, name)
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(errorMessage = error.message)
            }
        }
    }

    fun setFavorite(id: String, favorite: Boolean) {
        val keys = sessionManager.getActiveKeys() ?: return
        viewModelScope.launch {
            vaultRepository.setFavorite(keys, id, favorite)
        }
    }

    fun moveToTrash(id: String) {
        val keys = sessionManager.getActiveKeys() ?: return
        viewModelScope.launch {
            vaultRepository.moveToTrash(keys, id)
            if (_uiState.value.selectedDocumentId == id) {
                _uiState.value = _uiState.value.copy(selectedDocumentId = null, selectedDocument = null)
            }
        }
    }

    fun restoreFromTrash(id: String) {
        val keys = sessionManager.getActiveKeys() ?: return
        viewModelScope.launch {
            vaultRepository.restoreFromTrash(keys, id)
        }
    }

    fun permanentlyDelete(id: String) {
        val keys = sessionManager.getActiveKeys() ?: return
        viewModelScope.launch {
            vaultRepository.permanentlyDelete(keys, id)
        }
    }

    fun emptyTrash() {
        val keys = sessionManager.getActiveKeys() ?: return
        viewModelScope.launch {
            vaultRepository.emptyTrash(keys)
        }
    }

    fun openDocument(id: String) {
        val keys = sessionManager.getActiveKeys() ?: return
        val doc = vaultRepository.getDocument(keys, id)
        _uiState.value = _uiState.value.copy(selectedDocumentId = id, selectedDocument = doc)
    }

    fun closeDocument() {
        _uiState.value = _uiState.value.copy(selectedDocumentId = null, selectedDocument = null)
    }

    fun openTrash() {
        _uiState.value = _uiState.value.copy(isViewingTrash = true)
    }

    fun closeTrash() {
        _uiState.value = _uiState.value.copy(isViewingTrash = false)
    }

    fun setSearchQuery(query: String) {
        val keys = sessionManager.getActiveKeys() ?: return
        val results = if (query.isNotBlank()) vaultRepository.searchDocuments(keys, query) else emptyList()
        _uiState.value = _uiState.value.copy(searchQuery = query, searchResults = results)
    }

    fun toggleSelectionMode(initialId: String? = null) {
        val newMode = !_uiState.value.selectionMode
        val selected = if (newMode && initialId != null) setOf(initialId) else emptySet()
        _uiState.value = _uiState.value.copy(selectionMode = newMode, selectedIds = selected)
    }

    fun toggleDocumentSelection(id: String) {
        val current = _uiState.value.selectedIds.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _uiState.value = _uiState.value.copy(selectedIds = current)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectionMode = false, selectedIds = emptySet())
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }

    fun reportBiometricError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun createBiometricEnrollmentCipher(): Cipher? {
        if (sessionManager.getActiveKeys() == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Unlock Vault before enabling fingerprint unlock")
            return null
        }
        return runCatching {
            biometricManager.createEnrollmentCipher()
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                errorMessage = error.message ?: "Fingerprint unlock could not be enabled"
            )
        }.getOrNull()
    }

    fun completeBiometricEnrollment(cipher: Cipher) {
        val keys = sessionManager.getActiveKeys() ?: return
        runCatching {
            biometricManager.completeEnrollment(cipher, keys.vmk)
        }.onSuccess {
            _uiState.value = _uiState.value.copy(
                biometricEnabled = true,
                errorMessage = null,
                infoMessage = "Fingerprint unlock enabled"
            )
        }.onFailure { error ->
            biometricManager.disable()
            _uiState.value = _uiState.value.copy(
                biometricEnabled = false,
                errorMessage = error.message ?: "Fingerprint unlock could not be enabled"
            )
        }
    }

    fun createBiometricUnlockCipher(): Cipher? =
        runCatching {
            biometricManager.createUnlockCipher()
        }.onFailure { error ->
            biometricManager.disable()
            _uiState.value = _uiState.value.copy(
                biometricEnabled = false,
                errorMessage = error.message ?: "Fingerprint unlock is unavailable; use your passcode"
            )
        }.getOrNull()

    fun completeBiometricUnlock(cipher: Cipher) {
        viewModelScope.launch {
            try {
                val vmk = withContext(Dispatchers.IO) {
                    biometricManager.completeUnlock(cipher)
                }
                sessionManager.unlockWithVmk(vmk)
                _uiState.value = _uiState.value.copy(errorMessage = null)
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(
                    biometricEnabled = biometricManager.isEnabled(),
                    errorMessage = "Fingerprint unlock failed; use your passcode"
                )
            }
        }
    }

    fun disableBiometricUnlock() {
        biometricManager.disable()
        _uiState.value = _uiState.value.copy(
            biometricEnabled = false,
            infoMessage = "Fingerprint unlock disabled"
        )
    }
}
