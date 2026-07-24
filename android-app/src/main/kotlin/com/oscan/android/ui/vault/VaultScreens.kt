package com.oscan.android.ui.vault

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
import androidx.compose.material.icons.filled.Search
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
                            viewModel.reportBiometricError("Fingerprint authentication did not provide a secure key")
                            return
                        }
                        when (action) {
                            BiometricAction.ENROLL -> viewModel.completeBiometricEnrollment(cipher)
                            BiometricAction.UNLOCK -> viewModel.completeBiometricUnlock(cipher)
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
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

    return remember(prompt) {
        { action, cipher ->
            if (prompt == null) {
                viewModel.reportBiometricError("Fingerprint authentication is unavailable in this screen")
            } else {
                pendingAction = action
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(
                        if (action == BiometricAction.ENROLL) {
                            "Enable fingerprint unlock"
                        } else {
                            "Unlock Vault"
                        }
                    )
                    .setSubtitle("Confirm your fingerprint to access the encrypted Vault key")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText(if (action == BiometricAction.ENROLL) "Cancel" else "Use passcode")
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
    var isViewingSettings by remember { mutableStateOf(false) }
    var moveOutDocument by remember { mutableStateOf<VaultDocument?>(null) }
    val launchBiometricPrompt = rememberVaultBiometricPrompt(viewModel)

    LaunchedEffect(Unit) {
        viewModel.refreshConfiguredState()
    }
    LaunchedEffect(uiState.sessionState) {
        if (uiState.sessionState !is VaultSessionState.Unlocked) {
            isViewingSettings = false
            moveOutDocument = null
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
            isViewingSettings -> isViewingSettings = false
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
                onResetVault = viewModel::resetVault,
                onBack = onExit
            )
        }

        uiState.sessionState is VaultSessionState.Unlocked -> {
            val keys = (uiState.sessionState as VaultSessionState.Unlocked).keys
            val selectedDocument = uiState.selectedDocument
            when {
                isViewingSettings -> VaultSettingsScreen(
                    viewModel = viewModel,
                    onEnableBiometric = {
                        viewModel.createBiometricEnrollmentCipher()?.let { cipher ->
                            launchBiometricPrompt(BiometricAction.ENROLL, cipher)
                        }
                    },
                    onBack = { isViewingSettings = false }
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
                    onOpenSettings = { isViewingSettings = true },
                    onMoveOutRequested = { moveOutDocument = it }
                )
            }
        }
    }

    moveOutDocument?.let { document ->
        AlertDialog(
            onDismissRequest = { moveOutDocument = null },
            title = { Text("Move out of Vault?") },
            text = {
                Text("This document will be moved to the main library and will no longer be protected by Vault encryption.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.moveDocumentOutOfVault(document.id, null)
                        moveOutDocument = null
                    }
                ) {
                    Text("Move to library")
                }
            },
            dismissButton = {
                TextButton(onClick = { moveOutDocument = null }) {
                    Text("Cancel")
                }
            }
        )
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
                title = { Text("Set up Vault") },
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

            Text("Protect Sensitive Documents", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Documents in Vault are encrypted on this device. Access requires a 6–12 digit passcode.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = passcode,
                onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) passcode = it },
                label = { Text("Create passcode (6–12 digits)") },
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
                label = { Text("Confirm passcode") },
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
                        "I understand that if I forget my passcode, OScan cannot recover my Vault documents.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            val activeError = localError ?: errorMessage
            if (activeError != null) {
                Spacer(Modifier.height(12.dp))
                Text(activeError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
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
                Text("Create Vault")
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
    onResetVault: () -> Unit,
    onBack: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    val lockoutRemaining = (sessionState as? VaultSessionState.Locked)?.lockoutRemainingSeconds ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unlock Vault") },
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

            Text("Vault is Locked", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Enter your Vault passcode to view encrypted documents.",
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
                    Text("Unlock with fingerprint")
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
                        "Too many failed attempts. Try again in $lockoutRemaining seconds.",
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
                label = { Text("Passcode") },
                singleLine = true,
                enabled = lockoutRemaining == 0,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
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
                Text("Unlock Vault")
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = { showResetDialog = true }) {
                Text("Forgot passcode?")
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset & Delete Vault?") },
            text = {
                Text("If you forgot your passcode, OScan cannot recover your documents. Resetting Vault will permanently delete all encrypted Vault documents and reset Vault.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetVault()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset and Delete Vault")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
    onOpenSettings: () -> Unit,
    onMoveOutRequested: (VaultDocument) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.infoMessage) {
        val message = uiState.errorMessage ?: uiState.infoMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
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
                title = { Text("Vault") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { searchOpen = !searchOpen }) {
                        Icon(Icons.Default.Search, "Search Vault")
                    }
                    IconButton(onClick = onOpenTrash) {
                        Icon(Icons.Default.Delete, "Vault Trash")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.MoreVert, "Vault Settings")
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
                    "Encrypting and moving document into Vault…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (searchOpen) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Search Vault documents...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            val displayList = if (uiState.searchQuery.isNotBlank()) uiState.searchResults else uiState.documents

            if (displayList.isEmpty()) {
                EmptyStateLayout(
                    icon = Icons.Default.LockOpen,
                    title = "Vault is empty",
                    supportingText = "Move existing documents into Vault or scan/import directly to keep them encrypted."
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
                            if (document.isFavorite) "Remove from Vault favorites" else "Add to Vault favorites"
                        )
                    }
                    IconButton(onClick = onMoveOut) {
                        Icon(Icons.Default.LockOpen, "Move out of Vault")
                    }
                    IconButton(onClick = onTrash) {
                        Icon(Icons.Default.Delete, "Move to Vault Trash")
                    }
                }
            )
        }
    ) { padding ->
        if (document.pages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyStateLayout(
                    icon = Icons.Default.BrokenImage,
                    title = "No pages",
                    supportingText = "This Vault document does not contain any readable pages."
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
                                        contentDescription = "Page ${page.position + 1}",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.BrokenImage,
                                        contentDescription = "Page ${page.position + 1} could not be displayed",
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
                    Text("${doc.pages.size} page(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, "More actions")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(if (doc.isFavorite) "Unfavorite" else "Favorite") },
                            onClick = { menuOpen = false; onToggleFavorite() },
                            leadingIcon = { Icon(if (doc.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Move out of Vault") },
                            onClick = { menuOpen = false; onMoveOut() },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Vault Trash") },
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
                title = { Text("Vault Trash") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (trashDocuments.isNotEmpty()) {
                        TextButton(onClick = { showEmptyConfirm = true }) {
                            Text("Empty Trash", color = MaterialTheme.colorScheme.error)
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
                    title = "Vault Trash is empty",
                    supportingText = "Items deleted from Vault will stay here until emptied."
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
                                    Text("${doc.pages.size} page(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            title = { Text("Empty Vault Trash?") },
            text = { Text("All items in Vault Trash will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyConfirm = false
                        onEmptyTrash()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Empty Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSettingsScreen(
    viewModel: VaultViewModel,
    onEnableBiometric: () -> Unit = {},
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showChangePasscodeDialog by remember { mutableStateOf(false) }
    var showDisableVaultDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault Settings") },
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
                .padding(24.dp)
        ) {
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
                                onEnableBiometric()
                            }
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Unlock with fingerprint", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (uiState.biometricEnabled) {
                                    "Use your enrolled fingerprint for quick unlock"
                                } else if (!uiState.biometricAvailable) {
                                    "Enroll a strong fingerprint in Android settings first"
                                } else {
                                    "Your Vault passcode remains available as a fallback"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.biometricEnabled,
                            enabled = uiState.biometricEnabled || uiState.biometricAvailable,
                            onCheckedChange = {
                                if (it) onEnableBiometric() else viewModel.disableBiometricUnlock()
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
                        Text("Change Vault Passcode", style = MaterialTheme.typography.titleMedium)
                        Text("Re-wrap encryption keys with a new passcode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            uiState.errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
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
                        Text("Disable Vault", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Text("Migrate documents to library or delete Vault repository", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            title = { Text("Change Vault Passcode") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = curPass,
                        onValueChange = { curPass = it },
                        label = { Text("Current passcode") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New passcode (6-12 digits)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    OutlinedTextField(
                        value = confPass,
                        onValueChange = { confPass = it },
                        label = { Text("Confirm new passcode") },
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
                    Text("Change Passcode")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasscodeDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDisableVaultDialog) {
        var curPass by remember { mutableStateOf("") }
        var migrateChoice by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showDisableVaultDialog = false },
            title = { Text("Disable Vault") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose how to proceed with your Vault documents:")
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { migrateChoice = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = migrateChoice, onClick = { migrateChoice = true })
                        Spacer(Modifier.width(8.dp))
                        Text("Move documents to library & disable", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { migrateChoice = false },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = !migrateChoice, onClick = { migrateChoice = false })
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Vault and all documents", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    OutlinedTextField(
                        value = curPass,
                        onValueChange = { curPass = it },
                        label = { Text("Enter current passcode") },
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
                    Text("Disable Vault")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableVaultDialog = false }) { Text("Cancel") }
            }
        )
    }
}
