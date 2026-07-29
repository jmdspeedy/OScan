package com.oscan.android

import android.app.Application
import com.oscan.android.data.db.OScanDatabase
import com.oscan.android.data.preferences.UserPreferencesStore
import com.oscan.android.data.repository.DocumentRepository
import com.oscan.android.data.repository.LocalDocumentRepository
import com.oscan.android.data.session.ScanSessionStore
import com.oscan.android.data.storage.DocumentFileStore
import com.oscan.android.data.vault.LocalVaultRepository
import com.oscan.android.data.vault.VaultBiometricManager
import com.oscan.android.data.vault.VaultRepository
import com.oscan.android.data.vault.VaultSessionManager
import com.oscan.android.localization.AndroidAppLocaleController
import com.oscan.android.localization.AppLocaleController

class OScanApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        appContainer.fileStore.clearInterruptedWrites()
        appContainer.vaultRepository.cleanEphemeralCache()
    }
}

class AppContainer(application: Application) {
    val database: OScanDatabase by lazy { OScanDatabase.getInstance(application) }
    val fileStore: DocumentFileStore by lazy { DocumentFileStore(application) }
    val documentRepository: DocumentRepository by lazy {
        LocalDocumentRepository(database, fileStore)
    }
    val userPreferences: UserPreferencesStore by lazy { UserPreferencesStore(application) }
    val scanSessionStore: ScanSessionStore by lazy { ScanSessionStore(application) }
    val vaultRepository: VaultRepository by lazy { LocalVaultRepository(application) }
    val vaultSessionManager: VaultSessionManager by lazy { VaultSessionManager() }
    val vaultBiometricManager: VaultBiometricManager by lazy { VaultBiometricManager(application) }
    val appLocaleController: AppLocaleController by lazy { AndroidAppLocaleController() }
}
