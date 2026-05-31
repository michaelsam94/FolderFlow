package com.michael.folderflow

import android.app.Application
import com.michael.folderflow.core.di.AppContainer
import com.michael.folderflow.core.di.AppContainerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FolderFlowApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)

        // Seed default folders and scan/sync installed applications in the background thread
        applicationScope.launch {
            try {
                container.folderRepository.seedDefaultSystemFolders()
                container.appRepository.scanAndSyncApps(mockIfLowCount = true)
            } catch (e: Exception) {
                // Ignore seeding crashes in environment
            }
        }
    }
}
