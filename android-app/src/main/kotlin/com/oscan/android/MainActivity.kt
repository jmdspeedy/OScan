package com.oscan.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oscan.android.engine.AndroidScannerEngine
import com.oscan.android.ui.ScannerApp
import com.oscan.android.ui.ScannerViewModel
import com.oscan.android.ui.LibraryViewModel
import com.oscan.android.ui.CameraViewModel
import com.oscan.android.ui.theme.OScanTheme

class MainActivity : ComponentActivity() {

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
                    contentResolver = contentResolver
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OScanTheme {
                Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    ScannerApp(
                        viewModel = viewModel,
                        cameraViewModel = cameraViewModel,
                        libraryViewModel = libraryViewModel,
                        fileStore = container.fileStore
                    )
                }
            }
        }
    }
}
