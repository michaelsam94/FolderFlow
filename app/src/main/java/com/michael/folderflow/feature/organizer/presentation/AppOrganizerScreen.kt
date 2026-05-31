package com.michael.folderflow.feature.organizer.presentation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.michael.folderflow.core.domain.AppModel
import com.michael.folderflow.core.domain.PlayStoreCategories
import com.michael.folderflow.feature.folders.presentation.SectionHeader
import com.michael.folderflow.ui.theme.AccentAmber
import com.michael.folderflow.ui.theme.AccentEmerald
import com.michael.folderflow.ui.theme.ColorMuted
import java.text.DateFormat
import java.util.Date

private const val AppOrganizerLogTag = "FolderFlowAppDetail"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOrganizerScreen(
    viewModel: AppOrganizerViewModel,
    onOpenAppDetails: (String) -> Unit = {}
) {
    val listState by viewModel.listUiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortField by viewModel.sortField.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.refreshInstalledApps()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshInstalledApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(
                title = "App Index",
                subtitle = "Classification & layout directory"
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Text search box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = ColorMuted) },
                placeholder = { Text("Search package or app name...", color = ColorMuted) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (val state = listState) {
                is AppOrganizerUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is AppOrganizerUiState.Success -> {
                    // Quick category selection chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { viewModel.setSelectedCategory(cat) },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OrganizerSortRow(
                        currentField = sortField,
                        currentDirection = sortDirection,
                        onSortChanged = { field -> viewModel.toggleSort(field) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.apps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No apps match filtering criteria", fontWeight = FontWeight.Bold, color = ColorMuted)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(state.apps, key = { it.packageName }) { app ->
                                AppIndexItem(
                                    app = app,
                                    onClick = { onOpenAppDetails(app.packageName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrganizerSortRow(
    currentField: String,
    currentDirection: String,
    onSortChanged: (String) -> Unit
) {
    val sorts = listOf(
        "name" to "Name",
        "package" to "Package",
        "category" to "Category",
        "size" to "Size"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(sorts) { (field, label) ->
            val selected = currentField == field
            FilterChip(
                selected = selected,
                onClick = { onSortChanged(field) },
                label = {
                    Text(
                        text = if (selected) {
                            "$label ${if (currentDirection == "ASC") "ASC" else "DESC"}"
                        } else {
                            label
                        },
                        fontSize = 12.sp
                    )
                }
            )
        }
    }
}

@Composable
fun AppIndexItem(
    app: AppModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .semantics { contentDescription = "Open details for ${app.label}" }
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Circular logo representation
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.label.take(1).uppercase(),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = app.packageName,
                fontSize = 10.sp,
                color = ColorMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Override pill indicator
        val overrideActive = app.customCategory != null
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (overrideActive) AccentAmber.copy(alpha = 0.15f)
                    else AccentEmerald.copy(alpha = 0.12f)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = app.displayCategory + (if (overrideActive) " ✎" else ""),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (overrideActive) AccentAmber else AccentEmerald
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.Default.ArrowForward, contentDescription = "Open Details", tint = ColorMuted)
    }
}

@Composable
fun AppDetailRoute(
    viewModel: AppOrganizerViewModel,
    packageName: String,
    onBack: () -> Unit
) {
    val app by viewModel.app(packageName).collectAsState(initial = null)
    val listState by viewModel.listUiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var uninstallRequested by remember(packageName) { mutableStateOf(false) }

    LaunchedEffect(app, listState, uninstallRequested) {
        if (uninstallRequested && app == null && listState !is AppOrganizerUiState.Loading) {
            Log.d(AppOrganizerLogTag, "Closing detail after app row disappeared: $packageName")
            uninstallRequested = false
            onBack()
        }
    }

    DisposableEffect(lifecycleOwner, packageName, uninstallRequested) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && uninstallRequested) {
                val installed = AndroidAppSystemActions(context).isInstalled(packageName)
                Log.d(AppOrganizerLogTag, "Resume after uninstall request for $packageName installed=$installed")
                if (!installed) {
                    viewModel.removeApp(packageName)
                    uninstallRequested = false
                    onBack()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (app == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (listState is AppOrganizerUiState.Loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Text("App not found", color = ColorMuted)
            }
        }
    } else {
        AppDetailScreen(
            app = app!!,
            actions = AndroidAppSystemActions(context),
            onBack = onBack,
            onUninstallRequested = {
                Log.d(AppOrganizerLogTag, "Uninstall requested for $packageName")
                uninstallRequested = true
            },
            onOverrideCategory = { category ->
                viewModel.overrideCategory(packageName, category)
            }
        )
    }
}

@Composable
fun AppDetailScreen(
    app: AppModel,
    actions: AppSystemActions,
    onBack: () -> Unit,
    onUninstallRequested: () -> Unit = {},
    onOverrideCategory: ((String?) -> Unit)? = null
) {
    var editingCategory by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                Text("Back")
            }
            AppDetailHeader(app)

            AppDetailSection(title = "Resource usage") {
                StatGrid(
                    listOf(
                        "Storage" to formatBytes(app.sizeBytes),
                        "Usage score" to "${app.usageScore.toInt()}%",
                        "Last used" to formatDate(app.lastUsed)
                    )
                )
            }

            AppDetailSection(title = "App management") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { actions.openSettings(app.packageName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Open ${app.label} settings" }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open app settings")
                    }
                    OutlinedButton(
                        onClick = { actions.openStorageSettings(app.packageName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Open ${app.label} storage settings" }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Storage & cache")
                    }
                    OutlinedButton(
                        onClick = {
                            actions.requestUninstall(app.packageName)
                            onUninstallRequested()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Uninstall ${app.label}" }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uninstall")
                    }
                }
            }

            if (onOverrideCategory != null) {
                AppDetailSection(title = "Classification") {
                    Text(
                        text = "Current classification: ${app.displayCategory}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { editingCategory = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Override classification")
                    }
                }
            }

            AppDetailSection(title = "Package") {
                Text(app.packageName, style = MaterialTheme.typography.bodyMedium, color = ColorMuted)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Installed ${formatDate(app.installDate)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (editingCategory && onOverrideCategory != null) {
        EditCategoryBottomSheet(
            app = app,
            onDismiss = { editingCategory = false },
            onOverride = { category ->
                onOverrideCategory(category)
                editingCategory = false
            }
        )
    }
}

@Composable
private fun AppDetailHeader(app: AppModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.label.take(1).uppercase(),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(app.displayCategory, style = MaterialTheme.typography.bodyMedium, color = AccentEmerald)
        }
    }
}

@Composable
private fun AppDetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun StatGrid(stats: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(10.dp)
                    ) {
                        Text(label, fontSize = 12.sp, color = ColorMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

interface AppSystemActions {
    fun openSettings(packageName: String)
    fun openStorageSettings(packageName: String)
    fun requestUninstall(packageName: String)
    fun isInstalled(packageName: String): Boolean
}

class AndroidAppSystemActions(private val context: Context) : AppSystemActions {
    override fun openSettings(packageName: String) {
        context.startActivitySafely(appSettingsIntent(packageName))
    }

    override fun openStorageSettings(packageName: String) {
        context.startActivitySafely(appSettingsIntent(packageName))
    }

    override fun requestUninstall(packageName: String) {
        if (!isInstalled(packageName)) {
            Log.d(AppOrganizerLogTag, "Uninstall blocked because package is not installed: $packageName")
            Toast.makeText(context, "This app is not installed on this device.", Toast.LENGTH_SHORT).show()
            return
        }

        val launched = context.startActivitySafely(
            Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
            }
        )
        if (!launched) {
            Log.d(AppOrganizerLogTag, "Unable to launch uninstall screen for $packageName")
            Toast.makeText(context, "Unable to open uninstall screen.", Toast.LENGTH_SHORT).show()
        } else {
            Log.d(AppOrganizerLogTag, "Launched uninstall screen for $packageName")
        }
    }

    private fun appSettingsIntent(packageName: String) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
    }

    override fun isInstalled(packageName: String): Boolean {
        return context.isPackageInstalled(packageName)
    }
}

private fun Context.startActivitySafely(intent: Intent): Boolean {
    return runCatching {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}

private fun Context.isPackageInstalled(packageName: String): Boolean {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    }.isSuccess
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024f * 1024f)
    return if (mb >= 1024f) {
        "%.1f GB".format(mb / 1024f)
    } else {
        "%.1f MB".format(mb)
    }
}

private fun formatDate(timestamp: Long): String {
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoryBottomSheet(
    app: AppModel,
    onDismiss: () -> Unit,
    onOverride: (String?) -> Unit
) {
    val categories = PlayStoreCategories.all.map { it.name }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Override Classification",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Override app category manually for ${app.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorMuted
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text("Current classification: ${app.displayCategory}", fontSize = 12.sp, color = ColorMuted)
            Spacer(modifier = Modifier.height(12.dp))

            // Category rows
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                items(categories.chunked(2)) { rowCats ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowCats.forEach { cat ->
                            val isActive = app.displayCategory == cat
                            OutlinedButton(
                                onClick = { onOverride(cat) },
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = "Override category $cat" },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                                ),
                                border = ButtonDefaults.outlinedButtonBorder(true).run {
                                    if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    else this
                                },
                            ) {
                                Text(cat, color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (app.customCategory != null) {
                TextButton(
                    onClick = { onOverride(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset to system classification", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
