package com.michael.folderflow.core.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.michael.folderflow.FolderFlowApplication
import com.michael.folderflow.feature.widget.presentation.FolderWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PackageUpdatesReceiver : BroadcastReceiver() {
    private companion object {
        const val LogTag = "FolderFlowPackages"
    }

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
                    val packageName = intent.data?.schemeSpecificPart
                    val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                    Log.d(LogTag, "Package update action=$action package=$packageName replacing=$replacing")
                    if (action == Intent.ACTION_PACKAGE_REMOVED && !replacing && packageName != null) {
                        container?.appRepository?.removeApp(packageName)
                    } else {
                        container?.appRepository?.scanAndSyncApps(mockIfLowCount = false)
                    }
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
