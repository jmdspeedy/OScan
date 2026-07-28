package com.oscan.android.ui.vault

import com.oscan.android.R

import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oscan.android.data.vault.ActiveVaultKeys
import com.oscan.android.data.vault.VaultDocument
import com.oscan.android.data.vault.VaultRepository
import com.oscan.android.data.vault.VaultSessionState
import com.oscan.android.data.model.Document
import com.oscan.android.ui.EmptyStateLayout
import com.oscan.android.ui.localizedRuntimeMessage
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.Cipher

private enum class BiometricAction {
    ENROLL,
    UNLOCK
}

@Composable
private fun rememberVaultBiometricPrompt(
    viewModel: VaultViewModel
): (BiometricAction, Cipher) -> Unit {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }
    var pendingAction by remember { mutableStateOf<BiometricAction?>(null) }
    val currentAction = rememberUpdatedState(pendingAction)
    val secureKeyMissing = stringResource(R.string.biometric_secure_key_missing)
    val biometricUnavailable = stringResource(R.string.biometric_unavailable)
    val biometricSubtitle = stringResource(R.string.biometric_subtitle)
    val enableFingerprint = stringResource(R.string.vault_fingerprint_enable)
    val unlockVault = stringResource(R.string.vault_unlock)
    val cancel = stringResource(R.string.action_cancel)
    val usePasscode = stringResource(R.string.biometric_use_passcode)

    val prompt = remember(activity, executor, viewModel) {
        activity?.let { host ->
            BiometricPrompt(
                host,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val cipher = result.cryptoObject?.cipher
                        val action = currentAction.value
                        pendingAction = null
                        if (cipher == null || action == null) {
                            viewModel.reportBiometricError(secureKeyMissing)
                            return
                        }
                        when (action) {
                            BiometricAction.ENROLL -> viewModel.completeBiometricEnrollment(cipher)
                            BiometricAction.UNLOCK -> viewModel.completeBiometricUnlock(cipher)
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (currentAction.value == BiometricAction.ENROLL) {
                            viewModel.cancelBiometricEnrollment()
                        }
                        pendingAction = null
                        if (
                            errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                            errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.ERROR_CANCELED
                        ) {
                            viewModel.reportBiometricError(errString.toString())
                        }
                    }
                }
            )
        }
    }

    return remember(prompt, biometricUnavailable, biometricSubtitle, enableFingerprint, unlockVault, cancel, usePasscode) {
        { action, cipher ->
            if (prompt == null) {
                viewModel.reportBiometricError(biometricUnavailable)
            } else {
                pendingAction = action
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(
                        if (action == BiometricAction.ENROLL) {
                            enableFingerprint
                        } else {
                            unlockVault
                        }
                    )
                    .setSubtitle(biometricSubtitle)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText(if (action == BiometricAction.ENROLL) cancel else usePasscode)
                    .setConfirmationRequired(false)
                    .build()
                prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            }
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
fun VaultFlow(
    viewModel: VaultViewModel,
    documentToMoveIntoVault: Document? = null,
    documentsToMoveIntoVault: List<Document> = emptyList(),
    onMoveIntoVaultConsumed: () -> Unit = {},
    onExit: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var moveOutDocument by remember { mutableStateOf<VaultDocument?>(null) }
    var biometricPromptAttempted by remember { mutableStateOf(false) }
    val launchBiometricPrompt = rememberVaultBiometricPrompt(viewModel)

    LaunchedEffect(Unit) {
        viewModel.refreshConfiguredState()
    }
    LaunchedEffect(uiState.sessionState) {
        if (uiState.sessionState !is VaultSessionState.Unlocked) {
            moveOutDocument = null
        } else {
            biometricPromptAttempted = false
        }
    }
    LaunchedEffect(uiState.sessionState, uiState.biometricEnabled) {
        if (
            uiState.sessionState is VaultSessionState.Locked &&
            uiState.biometricEnabled &&
            !biometricPromptAttempted
        ) {
            biometricPromptAttempted = true
            viewModel.createBiometricUnlockCipher()?.let { cipher ->
                launchBiometricPrompt(BiometricAction.UNLOCK, cipher)
            }
        }
    }
    LaunchedEffect(
        uiState.sessionState is VaultSessionState.Unlocked,
        documentToMoveIntoVault?.id,
        documentsToMoveIntoVault
    ) {
        if (uiState.sessionState is VaultSessionState.Unlocked) {
            if (documentsToMoveIntoVault.isNotEmpty()) {
                viewModel.moveDocumentsIntoVault(documentsToMoveIntoVault)
                onMoveIntoVaultConsumed()
            } else if (documentToMoveIntoVault != null) {
                viewModel.moveDocumentIntoVault(documentToMoveIntoVault)
                onMoveIntoVaultConsumed()
            }
        }
    }

    BackHandler {
        when {
            uiState.isViewingTrash -> viewModel.closeTrash()
            uiState.selectedDocumentId != null -> viewModel.closeDocument()
            else -> onExit()
        }
    }

    when {
        !uiState.isConfigured || uiState.sessionState is VaultSessionState.NotConfigured -> {
            VaultSetupScreen(
                errorMessage = uiState.errorMessage,
                onSetupCompleted = viewModel::setupVault,
                onCancel = onExit
            )
        }

        uiState.sessionState is VaultSessionState.Locked -> {
            VaultUnlockScreen(
                sessionState = uiState.sessionState,
                errorMessage = uiState.errorMessage,
                biometricEnabled = uiState.biometricEnabled,
                onUnlock = viewModel::unlockVault,
                onBiometricUnlock = {
                    viewModel.createBiometricUnlockCipher()?.let { cipher ->
                        launchBiometricPrompt(BiometricAction.UNLOCK, cipher)
                    }
                },
                onBack = onExit
            )
        }

        uiState.sessionState is VaultSessionState.Unlocked -> {
            val keys = (uiState.sessionState as VaultSessionState.Unlocked).keys
            val selectedDocument = uiState.selectedDocument
            when {
                uiState.showBiometricSetup -> VaultBiometricSetupScreen(
                    biometricAvailable = uiState.biometricAvailable,
                    errorMessage = uiState.errorMessage,
                    onEnable = {
                        viewModel.createBiometricEnrollmentCipher()?.let { cipher ->
                            launchBiometricPrompt(BiometricAction.ENROLL, cipher)
                        }
                    },
                    onSkip = viewModel::skipBiometricSetup
                )

                uiState.isViewingTrash -> VaultTrashScreen(
                    trashDocuments = uiState.trashDocuments,
                    onRestore = viewModel::restoreFromTrash,
                    onPermanentlyDelete = viewModel::permanentlyDelete,
                    onEmptyTrash = viewModel::emptyTrash,
                    onBack = viewModel::closeTrash
                )

                selectedDocument != null -> VaultDocumentScreen(
                    document = selectedDocument,
                    repository = viewModel.vaultRepository,
                    keys = keys,
                    onBack = viewModel::closeDocument,
                    onToggleFavorite = {
                        viewModel.setFavorite(
                            selectedDocument.id,
                            !selectedDocument.isFavorite
                        )
                    },
                    onMoveOut = { moveOutDocument = selectedDocument },
                    onTrash = { viewModel.moveToTrash(selectedDocument.id) }
                )

                else -> VaultLibraryScreen(
                    viewModel = viewModel,
                    repository = viewModel.vaultRepository,
                    keys = keys,
                    onBack = onExit,
                    onOpenTrash = viewModel::openTrash,
                    onMoveOutRequested = { moveOutDocument = it }
                )
            }
        }
    }

    moveOutDocument?.let { document ->
        AlertDialog(
            onDismissRequest = { moveOutDocument = null },
            title = { Text(stringResource(R.string.vault_move_out_title)) },
            text = {
                Text(stringResource(R.string.vault_move_out_body))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.moveDocumentOutOfVault(document.id, null)
                        moveOutDocument = null
                    }
                ) {
                    Text(stringResource(R.string.vault_move_library))
                }
            },
            dismissButton = {
                TextButton(onClick = { moveOutDocument = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultBiometricSetupScreen(
    biometricAvailable: Boolean,
    errorMessage: String?,
    onEnable: () -> Unit,
    onSkip: () -> Unit
) {
    BackHandler(onBack = onSkip)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.vault_fingerprint_setup)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.vault_fingerprint_faster),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (biometricAvailable) {
                    stringResource(R.string.vault_fingerprint_body)
                } else {
                    stringResource(R.string.vault_fingerprint_unavailable_body)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onEnable,
                enabled = biometricAvailable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vault_fingerprint_enable))
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text(if (biometricAvailable) "Not now" else "Continue")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSetupScreen(
    errorMessage: String?,
    onSetupCompleted: (String) -> Unit,
    onCancel: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    var showPasscode by remember { mutableStateOf(false) }
    var ackNoRecovery by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vault_setup_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancel setup")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.vault_protect_sensitive), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.vault_setup_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = passcode,
                onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) passcode = it },
                label = { Text(stringResource(R.string.vault_create_passcode)) },
                singleLine = true,
                visualTransformation = if (showPasscode) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                trailingIcon = {
                    IconButton(onClick = { showPasscode = !showPasscode }) {
                        Icon(if (showPasscode) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Show passcode")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPasscode,
                onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) confirmPasscode = it },
                label = { Text(stringResource(R.string.vault_confirm_passcode)) },
                singleLine = true,
                visualTransformation = if (showPasscode) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = ackNoRecovery, onCheckedChange = { ackNoRecovery = it })
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.vault_recovery_ack),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            val activeError = localError ?: errorMessage
            if (activeError != null) {
                Spacer(Modifier.height(12.dp))
                Text(localizedRuntimeMessage(activeError), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (passcode != confirmPasscode) {
                        localError = "Passcodes do not match"
                        return@Button
                    }
                    if (!ackNoRecovery) {
                        localError = "Please acknowledge the no-recovery warning"
                        return@Button
                    }
                    localError = null
                    onSetupCompleted(passcode)
                },
                enabled = passcode.length >= 6 && confirmPasscode.length >= 6 && ackNoRecovery,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.vault_create))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultUnlockScreen(
    sessionState: VaultSessionState,
    errorMessage: String?,
    biometricEnabled: Boolean = false,
    onUnlock: (String) -> Unit,
    onBiometricUnlock: () -> Unit = {},
    onBack: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }

    val lockoutRemaining = (sessionState as? VaultSessionState.Locked)?.lockoutRemainingSeconds ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vault_unlock)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.vault_locked_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.vault_unlock_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            if (biometricEnabled) {
                OutlinedButton(
                    onClick = onBiometricUnlock,
                    enabled = lockoutRemaining == 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.vault_fingerprint_unlock))
                }
                Spacer(Modifier.height(12.dp))
            }

            if (lockoutRemaining > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        stringResource(R.string.vault_lockout, lockoutRemaining),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            OutlinedTextField(
                value = passcode,
                onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) passcode = it },
                label = { Text(stringResource(R.string.vault_passcode)) },
                singleLine = true,
                enabled = lockoutRemaining == 0,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(localizedRuntimeMessage(errorMessage), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    onUnlock(passcode)
                    passcode = ""
                },
                enabled = passcode.length >= 6 && lockoutRemaining == 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.vault_unlock))
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultLibraryScreen(
    viewModel: VaultViewModel,
    repository: VaultRepository,
    keys: ActiveVaultKeys,
    onBack: () -> Unit,
    onOpenTrash: () -> Unit,
    onMoveOutRequested: (VaultDocument) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val rawSnackbarMessage = uiState.errorMessage ?: uiState.infoMessage
    val snackbarMessage = rawSnackbarMessage?.let { localizedRuntimeMessage(it) }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            if (uiState.errorMessage != null) {
                viewModel.clearErrorMessage()
            } else {
                viewModel.clearInfoMessage()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vault_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenTrash) {
                        Icon(Icons.Default.Delete, "Vault Trash")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isMovingDocument) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    stringResource(R.string.vault_moving),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text(stringResource(R.string.vault_search_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val displayList = if (uiState.searchQuery.isNotBlank()) uiState.searchResults else uiState.documents

            if (displayList.isEmpty()) {
                EmptyStateLayout(
                    icon = Icons.Default.LockOpen,
                    title = stringResource(R.string.vault_empty_title),
                    supportingText = stringResource(R.string.vault_empty_body)
                ) {}
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = displayList, key = { it.id }) { doc ->
                        VaultDocumentItem(
                            doc = doc,
                            repository = repository,
                            keys = keys,
                            onOpen = { viewModel.openDocument(doc.id) },
                            onToggleFavorite = { viewModel.setFavorite(doc.id, !doc.isFavorite) },
                            onMoveOut = { onMoveOutRequested(doc) },
                            onTrash = { viewModel.moveToTrash(doc.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDocumentScreen(
    document: VaultDocument,
    repository: VaultRepository,
    keys: ActiveVaultKeys,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveOut: () -> Unit,
    onTrash: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        document.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (document.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            stringResource(
                                if (document.isFavorite) R.string.vault_favorite_remove else R.string.vault_favorite_add
                            )
                        )
                    }
                    IconButton(onClick = onMoveOut) {
                        Icon(Icons.Default.LockOpen, "Move out of Vault")
                    }
                    IconButton(onClick = onTrash) {
                        Icon(Icons.Default.Delete, stringResource(R.string.vault_move_trash))
                    }
                }
            )
        }
    ) { padding ->
        if (document.pages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyStateLayout(
                    icon = Icons.Default.BrokenImage,
                    title = stringResource(R.string.vault_no_pages),
                    supportingText = stringResource(R.string.vault_no_pages_body)
                ) {}
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(220.dp),
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(document.pages, key = { it.id }) { page ->
                    val pageBitmap by produceState<android.graphics.Bitmap?>(
                        initialValue = null,
                        document.id,
                        page.id,
                        page.processedAsset,
                        keys
                    ) {
                        value = withContext(Dispatchers.IO) {
                            runCatching {
                                repository.getDecryptedAssetStream(
                                    keys = keys,
                                    documentId = document.id,
                                    pageId = page.id,
                                    assetKind = "PROCESSED",
                                    assetPath = page.processedAsset
                                ).use(BitmapFactory::decodeStream)
                            }.getOrNull()
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column {
                            Box(
                                modifier = Modifier.fillMaxWidth().aspectRatio(
                                    if (page.height > 0) page.width.toFloat() / page.height.toFloat() else 0.75f
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (pageBitmap != null) {
                                    Image(
                                        bitmap = pageBitmap!!.asImageBitmap(),
                                        contentDescription = stringResource(R.string.vault_page_cd, page.position + 1),
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.BrokenImage,
                                        contentDescription = stringResource(R.string.vault_page_missing_cd, page.position + 1),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Text(
                                "Page ${page.position + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultDocumentItem(
    doc: VaultDocument,
    repository: VaultRepository,
    keys: ActiveVaultKeys,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveOut: () -> Unit,
    onTrash: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    val firstPage = doc.pages.firstOrNull()
    val thumbnailBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, firstPage?.id) {
        if (firstPage != null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    repository.getDecryptedAssetStream(keys, doc.id, firstPage.id, "THUMBNAIL", firstPage.thumbnailAsset).use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = doc.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Description, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(doc.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(pluralStringResource(R.plurals.plural_pages, doc.pages.size, doc.pages.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, "More actions")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (doc.isFavorite) R.string.favorite_remove else R.string.favorite_add
                                    )
                                )
                            },
                            onClick = { menuOpen = false; onToggleFavorite() },
                            leadingIcon = { Icon(if (doc.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.vault_move_out)) },
                            onClick = { menuOpen = false; onMoveOut() },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.vault_move_trash)) },
                            onClick = { menuOpen = false; onTrash() },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTrashScreen(
    trashDocuments: List<VaultDocument>,
    onRestore: (String) -> Unit,
    onPermanentlyDelete: (String) -> Unit,
    onEmptyTrash: () -> Unit,
    onBack: () -> Unit
) {
    var showEmptyConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vault_trash_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (trashDocuments.isNotEmpty()) {
                        TextButton(onClick = { showEmptyConfirm = true }) {
                            Text(stringResource(R.string.vault_empty_trash), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (trashDocuments.isEmpty()) {
                EmptyStateLayout(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.vault_trash_empty),
                    supportingText = stringResource(R.string.vault_trash_empty_body)
                ) {}
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trashDocuments, key = { it.id }) { doc ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(doc.name, style = MaterialTheme.typography.titleMedium)
                                    Text(pluralStringResource(R.plurals.plural_pages, doc.pages.size, doc.pages.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onRestore(doc.id) }) {
                                    Icon(Icons.Default.Restore, "Restore")
                                }
                                IconButton(onClick = { onPermanentlyDelete(doc.id) }) {
                                    Icon(Icons.Default.DeleteForever, "Delete permanently", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text(stringResource(R.string.vault_empty_trash_title)) },
            text = { Text(stringResource(R.string.vault_empty_trash_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyConfirm = false
                        onEmptyTrash()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.vault_empty_trash))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSettingsScreen(
    viewModel: VaultViewModel,
    onEnableBiometric: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val resetConfirmationPhrase = stringResource(R.string.vault_reset_confirmation_phrase)
    var showChangePasscodeDialog by remember { mutableStateOf(false) }
    var showDisableVaultDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showFinalResetDialog by remember { mutableStateOf(false) }
    var showBiometricPasscodeDialog by remember { mutableStateOf(false) }

    val requestBiometricEnrollment = {
        if (uiState.sessionState is VaultSessionState.Unlocked) {
            onEnableBiometric("")
        } else {
            showBiometricPasscodeDialog = true
        }
    }

    LaunchedEffect(Unit) { viewModel.refreshConfiguredState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vault_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            if (!uiState.isConfigured) {
                EmptyStateLayout(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.vault_not_configured_title),
                    supportingText = stringResource(R.string.vault_not_configured_body)
                ) {}
                return@Column
            }
            if (uiState.biometricSupported || uiState.biometricEnabled) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (uiState.biometricEnabled) {
                                viewModel.disableBiometricUnlock()
                            } else if (uiState.biometricAvailable) {
                                requestBiometricEnrollment()
                            }
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(stringResource(R.string.vault_fingerprint_unlock), style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (uiState.biometricEnabled) {
                                    stringResource(R.string.vault_fingerprint_enrolled)
                                } else if (!uiState.biometricAvailable) {
                                    stringResource(R.string.vault_fingerprint_enroll_first)
                                } else {
                                    stringResource(R.string.vault_passcode_fallback)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Switch(
                            checked = uiState.biometricEnabled,
                            enabled = uiState.biometricEnabled || uiState.biometricAvailable,
                            onCheckedChange = {
                                if (it) requestBiometricEnrollment() else viewModel.disableBiometricUnlock()
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showChangePasscodeDialog = true }
                    .padding(vertical = 4.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.vault_change_passcode), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.vault_change_passcode_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            uiState.errorMessage?.let { message ->
                Text(localizedRuntimeMessage(message), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDisableVaultDialog = true }
                    .padding(vertical = 4.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.vault_disable), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Text(stringResource(R.string.vault_disable_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().clickable { showResetDialog = true }.padding(vertical = 4.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.vault_forgot_passcode), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Text(stringResource(R.string.vault_forgot_passcode_settings_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showChangePasscodeDialog) {
        var curPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var confPass by remember { mutableStateOf("") }
        var dialogError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePasscodeDialog = false },
            title = { Text(stringResource(R.string.vault_change_passcode)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = curPass,
                        onValueChange = { curPass = it },
                        label = { Text(stringResource(R.string.vault_current_passcode)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text(stringResource(R.string.vault_new_passcode)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    OutlinedTextField(
                        value = confPass,
                        onValueChange = { confPass = it },
                        label = { Text(stringResource(R.string.vault_confirm_new_passcode)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    if (dialogError != null) {
                        Text(dialogError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPass != confPass) {
                        dialogError = "New passcodes do not match"
                        return@Button
                    }
                    viewModel.changePasscode(curPass, newPass)
                    showChangePasscodeDialog = false
                }) {
                    Text(stringResource(R.string.vault_change_passcode_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasscodeDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showBiometricPasscodeDialog) {
        var passcode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBiometricPasscodeDialog = false },
            icon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
            title = { Text(stringResource(R.string.vault_fingerprint_enable)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.vault_fingerprint_passcode_prompt))
                    OutlinedTextField(
                        value = passcode,
                        onValueChange = { if (it.length <= 12 && it.all(Char::isDigit)) passcode = it },
                        label = { Text(stringResource(R.string.vault_enter_current_passcode)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBiometricPasscodeDialog = false
                        onEnableBiometric(passcode)
                    },
                    enabled = passcode.length >= 6
                ) { Text(stringResource(R.string.action_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showBiometricPasscodeDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showDisableVaultDialog) {
        var curPass by remember { mutableStateOf("") }
        var migrateChoice by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showDisableVaultDialog = false },
            title = { Text(stringResource(R.string.vault_disable)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.vault_disable_prompt))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { migrateChoice = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = migrateChoice, onClick = { migrateChoice = true })
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.vault_disable_move), style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { migrateChoice = false },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = !migrateChoice, onClick = { migrateChoice = false })
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.vault_disable_delete), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    OutlinedTextField(
                        value = curPass,
                        onValueChange = { curPass = it },
                        label = { Text(stringResource(R.string.vault_enter_current_passcode)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.disableVault(curPass, migrateChoice)
                        showDisableVaultDialog = false
                        onBack()
                    },
                    enabled = curPass.length >= 6,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.vault_disable))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableVaultDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.vault_reset_title)) },
            text = { Text(stringResource(R.string.vault_reset_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        showFinalResetDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.vault_reset_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showFinalResetDialog) {
        var confirmation by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFinalResetDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.vault_reset_final_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.vault_reset_type_prompt, resetConfirmationPhrase))
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        label = { Text(resetConfirmationPhrase) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinalResetDialog = false
                        viewModel.resetVault()
                    },
                    enabled = confirmation == resetConfirmationPhrase,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.vault_reset_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showFinalResetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun VaultSettingsRoute(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val launchBiometricPrompt = rememberVaultBiometricPrompt(viewModel)
    VaultSettingsScreen(
        viewModel = viewModel,
        onEnableBiometric = { passcode ->
            if (passcode.isEmpty()) {
                viewModel.createBiometricEnrollmentCipher()?.let { cipher ->
                    launchBiometricPrompt(BiometricAction.ENROLL, cipher)
                }
            } else {
                viewModel.createBiometricEnrollmentCipher(passcode) { cipher ->
                    launchBiometricPrompt(BiometricAction.ENROLL, cipher)
                }
            }
        },
        onBack = onBack
    )
}
