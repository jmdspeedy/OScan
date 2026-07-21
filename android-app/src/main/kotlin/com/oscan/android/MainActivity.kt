package com.oscan.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oscan.android.engine.AndroidScannerEngine
import com.oscan.android.ui.ScannerApp
import com.oscan.android.ui.ScannerViewModel

class MainActivity : ComponentActivity() {

    private val scannerEngine by lazy { AndroidScannerEngine(applicationContext) }

    private val viewModel: ScannerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScannerViewModel(scannerEngine) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    ScannerApp(viewModel = viewModel)
                }
            }
        }
    }
}
