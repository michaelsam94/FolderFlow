package com.example.feature.widget.presentation

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.example.FolderFlowApplication
import com.example.R
import com.example.core.domain.FolderModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FolderWidgetProvider : AppWidgetProvider() {

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val ACTION_LAUNCH_APP = "com.example.FolderFlow.ACTION_LAUNCH_APP"
        private const val EXTRA_PACKAGE_NAME = "com.example.FolderFlow.EXTRA_PACKAGE_NAME"
        private const val EXTRA_APP_LABEL = "com.example.FolderFlow.EXTRA_APP_LABEL"
        
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, FolderWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, FolderWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        widgetScope.launch {
            try {
                val container = (context.applicationContext as? FolderFlowApplication)?.container
                val folders = container?.folderRepository?.getAllFolders()?.firstOrNull() ?: emptyList()
                
                // Use default system folder (e.g., Social, system_social) or first available
                val activeFolder = folders.find { it.id == "system_social" } ?: folders.firstOrNull()

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.folder_widget_layout)
                    
                    if (activeFolder != null) {
                        views.setTextViewText(R.id.widget_folder_icon, activeFolder.iconEmoji)
                        views.setTextViewText(R.id.widget_folder_name, "${activeFolder.name} Flow")
                        
                        // Clear container first (supported in RemoteViews since API 14+)
                        views.removeAllViews(R.id.widget_apps_container)

                        // Take first 4 dynamic apps to fit the widget bounds beautifully
                        val appsToDisplay = activeFolder.apps.take(4)
                        if (appsToDisplay.isEmpty()) {
                            val rowViews = RemoteViews(context.packageName, R.layout.widget_app_row)
                            rowViews.setTextViewText(R.id.widget_app_icon, "✨")
                            rowViews.setTextViewText(R.id.widget_app_label, "No apps in folder yet")
                            views.addView(R.id.widget_apps_container, rowViews)
                        } else {
                            for (app in appsToDisplay) {
                                val rowViews = RemoteViews(context.packageName, R.layout.widget_app_row)
                                
                                val emoji = when (app.displayCategory) {
                                    "Social" -> "💬"
                                    "Finance" -> "💵"
                                    "Games" -> "🎮"
                                    "Tools" -> "⚙️"
                                    "Shopping" -> "🛍️"
                                    "Health" -> "❤️"
                                    "Media" -> "🎬"
                                    "Utilities" -> "🛠️"
                                    else -> "📱"
                                }
                                
                                rowViews.setTextViewText(R.id.widget_app_icon, emoji)
                                rowViews.setTextViewText(R.id.widget_app_label, app.label)

                                // PendingIntent on individual app row
                                val clickIntent = Intent(context, FolderWidgetProvider::class.java).apply {
                                    action = ACTION_LAUNCH_APP
                                    putExtra(EXTRA_PACKAGE_NAME, app.packageName)
                                    putExtra(EXTRA_APP_LABEL, app.label)
                                }
                                
                                val pendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    app.packageName.hashCode(),
                                    clickIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                rowViews.setOnClickPendingIntent(R.id.widget_app_icon, pendingIntent)
                                rowViews.setOnClickPendingIntent(R.id.widget_app_label, pendingIntent)

                                views.addView(R.id.widget_apps_container, rowViews)
                            }
                        }
                    } else {
                        views.setTextViewText(R.id.widget_folder_name, "Launch FolderFlow")
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                // Squelch widget update issues to ensure stability
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_LAUNCH_APP) {
            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
            val label = intent.getStringExtra(EXTRA_APP_LABEL) ?: "App"
            if (packageName != null) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    try {
                        context.startActivity(launchIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "$label isn't installed. Run in FolderFlow Demo mode.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Show a gorgeous Toast for mock/simulated applications
                    Toast.makeText(context, "Launching simulated app: $label ($packageName)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
