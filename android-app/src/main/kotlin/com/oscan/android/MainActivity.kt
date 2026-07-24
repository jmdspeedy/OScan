package com.oscan.android

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.fragment.app.FragmentActivity
import com.oscan.android.engine.AndroidScannerEngine
import com.oscan.android.ui.CameraViewModel
import com.oscan.android.ui.LibraryViewModel
import com.oscan.android.ui.ScannerApp
import com.oscan.android.ui.ScannerViewModel
import com.oscan.android.ui.theme.OScanTheme
import com.oscan.android.ui.vault.VaultViewModel
import com.oscan.core.model.FilterType
import kotlinx.coroutines.flow.first

class MainActivity : FragmentActivity() {

    private val scannerEngine by lazy { AndroidScannerEngine(applicationContext) }
    private val container by lazy { (application as OScanApplication).appContainer }

    private val viewModel: ScannerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScannerViewModel(
                    scannerEngine = scannerEngine,
                    repository = container.documentRepository,
                    sessionStore = container.scanSessionStore,
                    contentResolver = contentResolver,
                    defaultFilterProvider = {
                        runCatching {
                            FilterType.valueOf(container.userPreferences.preferences.first().defaultTreatment)
                        }.getOrDefault(FilterType.MAGIC)
                    }
                ) as T
            }
        }
    }

    private val libraryViewModel: LibraryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LibraryViewModel(
                    repository = container.documentRepository,
                    preferencesStore = container.userPreferences
                ) as T
            }
        }
    }

    private val cameraViewModel: CameraViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CameraViewModel(application, scannerEngine) as T
        }
    }

    private val vaultViewModel: VaultViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                VaultViewModel(
                    vaultRepository = container.vaultRepository,
                    sessionManager = container.vaultSessionManager,
                    biometricManager = container.vaultBiometricManager,
                    fileStore = container.fileStore,
                    documentRepository = container.documentRepository
                ) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val uiState by libraryViewModel.uiState.collectAsState()
            OScanTheme(
                themeChoice = uiState.userPreferences.themeChoice,
                accentTheme = uiState.userPreferences.accentTheme
            ) {
                Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    ScannerApp(
                        viewModel = viewModel,
                        cameraViewModel = cameraViewModel,
                        libraryViewModel = libraryViewModel,
                        vaultViewModel = vaultViewModel,
                        fileStore = container.fileStore,
                        scannerEngine = scannerEngine,
                        repository = container.documentRepository,
                        onSecureWindowChanged = { secure ->
                            if (secure) {
                                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                            } else {
                                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                            }
                        }
                    )
                }
            }
        }
    }
}
