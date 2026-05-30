package com.example.core.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.FolderFlowApplication
import com.example.feature.widget.presentation.FolderWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PackageUpdatesReceiver : BroadcastReceiver() {
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_PACKAGE_ADDED ||
            action == Intent.ACTION_PACKAGE_REMOVED ||
            action == Intent.ACTION_PACKAGE_REPLACED) {
            
            val pendingResult = goAsync()
            receiverScope.launch {
                try {
                    val container = (context.applicationContext as? FolderFlowApplication)?.container
                    container?.appRepository?.scanAndSyncApps(mockIfLowCount = false)
                    FolderWidgetProvider.triggerUpdate(context)
                } catch (e: Exception) {
                    // Squelch background errors for stability
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
