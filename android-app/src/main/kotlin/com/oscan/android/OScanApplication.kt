package com.oscan.android

import android.app.Application
import com.oscan.android.data.db.OScanDatabase
import com.oscan.android.data.preferences.UserPreferencesStore
import com.oscan.android.data.repository.DocumentRepository
import com.oscan.android.data.repository.LocalDocumentRepository
import com.oscan.android.data.storage.DocumentFileStore
import com.oscan.android.data.session.ScanSessionStore

class OScanApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        appContainer.fileStore.clearInterruptedWrites()
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
}
