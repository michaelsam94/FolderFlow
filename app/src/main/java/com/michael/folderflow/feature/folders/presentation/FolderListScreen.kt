package com.michael.folderflow.feature.folders.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.michael.folderflow.core.domain.FolderModel
import com.michael.folderflow.core.domain.FolderRule
import com.michael.folderflow.core.domain.RuleType
import com.michael.folderflow.ui.theme.AccentEmerald
import com.michael.folderflow.ui.theme.ColorMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderListScreen(
    viewModel: FolderViewModel,
    onNavigateToDetail: (String) -> Unit
) {
    val listState by viewModel.listUiState.collectAsState()
    var showCreateSheet by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<FolderModel?>(null) }
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

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Create smart folder") },
                text = { Text("Smart Folder") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(
                title = "Live Folders",
                subtitle = "Automatic rule-based App categorization"
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = listState) {
                is FolderListUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is FolderListUiState.Success -> {
                    val folders = state.folders
                    if (folders.isEmpty()) {
                        EmptyFolderState()
                    } else {
                        // Quick Stats Header
                        ClassificationStatsHeader(folders)
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 96.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(folders) { folder ->
                                FolderGridCard(
                                    folder = folder,
                                    onClick = {
                                        viewModel.selectFolder(folder.id)
                                        onNavigateToDetail(folder.id)
                                    },
                                    onEdit = if (folder.isSystem) null else {
                                        { editingFolder = folder }
                                    },
                                    onDelete = if (folder.isSystem) null else {
                                        { viewModel.deleteFolder(folder.id) }
                                    }
                                )
                            }
                        }
                    }
                }
                is FolderListUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateFolderBottomSheet(
            onDismiss = { showCreateSheet = false },
            onCreate = { name, emoji, ruleType, value ->
                val mockRule = FolderRule(
                    id = "",
                    folderId = "",
                    type = ruleType,
                    value = value
                )
                viewModel.createFolder(name, emoji, "#3B82F6", listOf(mockRule))
                showCreateSheet = false
            }
        )
    }

    editingFolder?.let { folder ->
        CreateFolderBottomSheet(
            folder = folder,
            onDismiss = { editingFolder = null },
            onCreate = { _, _, _, _ -> },
            onUpdate = { folderId, name, emoji, ruleType, value ->
                val rule = FolderRule(
                    id = folder.rules.firstOrNull()?.id.orEmpty(),
                    folderId = folderId,
                    type = ruleType,
                    value = value
                )
                viewModel.updateFolder(folderId, name, emoji, folder.colorToken, listOf(rule))
                editingFolder = null
            },
            onDelete = { folderId ->
                viewModel.deleteFolder(folderId)
                editingFolder = null
            }
        )
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = ColorMuted
        )
    }
}

@Composable
fun ClassificationStatsHeader(folders: List<FolderModel>) {
    val totalAppsMapped = folders.sumOf { it.apps.size }
    val systemFoldersCount = folders.count { it.isSystem }
    val customFoldersCount = folders.count { !it.isSystem }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "STATUS: ACTIVE",
                    color = AccentEmerald,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Classification Engine Mapped",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$totalAppsMapped Apps",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$customFoldersCount Custom • $systemFoldersCount System",
                    fontSize = 11.sp,
                    color = ColorMuted
                )
            }
        }
    }
}

@Composable
fun FolderGridCard(
    folder: FolderModel,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val themeColor = try {
        Color(android.graphics.Color.parseColor(folder.colorToken))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp)
            .semantics { contentDescription = "Open folder ${folder.name}" }
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = folder.iconEmoji, fontSize = 20.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${folder.apps.size} apps",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(32.dp)
                                .semantics { contentDescription = "Edit folder ${folder.name}" }
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = ColorMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(32.dp)
                                .semantics { contentDescription = "Delete folder ${folder.name}" }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                val appsSample = folder.apps.take(2).map { it.label }.joinToString(", ")
                Text(
                    text = if (appsSample.isNotEmpty()) appsSample else "Empty folder",
                    fontSize = 11.sp,
                    color = ColorMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptyFolderState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Home,
            contentDescription = "Empty Folders",
            tint = ColorMuted,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No folders created yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Click the Action Button to establish dynamic smart folders based on categories or tags",
            style = MaterialTheme.typography.bodyMedium,
            color = ColorMuted,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun CardStroke(width: androidx.compose.ui.unit.Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFolderBottomSheet(
    folder: FolderModel? = null,
    onDismiss: () -> Unit,
    onCreate: (String, String, RuleType, String) -> Unit,
    onUpdate: (String, String, String, RuleType, String) -> Unit = { _, _, _, _, _ -> },
    onDelete: (String) -> Unit = {}
) {
    val existingRule = folder?.rules?.firstOrNull()
    var name by remember(folder?.id) { mutableStateOf(folder?.name.orEmpty()) }
    var emoji by remember(folder?.id) { mutableStateOf(folder?.iconEmoji.orEmpty()) }
    var ruleType by remember(folder?.id) { mutableStateOf(existingRule?.type ?: RuleType.CATEGORY) }
    var ruleValue by remember(folder?.id) { mutableStateOf(existingRule?.value.orEmpty()) }
    val isEditing = folder != null

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
                text = if (isEditing) "Edit Smart Folder" else "Establish Smart Folder",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder Name") },
                placeholder = { Text("e.g. Casual Gaming") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = emoji,
                onValueChange = { emoji = it },
                label = { Text("Folder Icon (Emoji)") },
                placeholder = { Text("e.g. 👾") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Classification Rule Builder", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Rule Type selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair(RuleType.CATEGORY, "Category"),
                    Pair(RuleType.TAG, "Tag"),
                    Pair(RuleType.INSTALL_RECENT, "Recent"),
                    Pair(RuleType.UNUSED_THRESHOLD, "Stale")
                ).forEach { (type, label) ->
                    FilterChip(
                        selected = ruleType == type,
                        onClick = { ruleType = type },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = ruleValue,
                onValueChange = { ruleValue = it },
                label = { 
                    Text(
                        when (ruleType) {
                            RuleType.CATEGORY -> "Category Value (e.g. Games, Social)"
                            RuleType.TAG -> "Tag Value (e.g. work, travel)"
                            RuleType.INSTALL_RECENT -> "Install date within how many days (e.g. 7)"
                            RuleType.UNUSED_THRESHOLD -> "Inactivity threshold in days (e.g. 30)"
                        }
                    )
                },
                placeholder = { Text("e.g. Games") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (folder != null) {
                TextButton(
                    onClick = { onDelete(folder.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Delete smart folder" }
                ) {
                    Text(
                        text = "Delete Smart Folder",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (folder == null) {
                        onCreate(name, emoji, ruleType, ruleValue)
                    } else {
                        onUpdate(folder.id, name, emoji, ruleType, ruleValue)
                    }
                },
                enabled = name.isNotBlank() && emoji.isNotBlank() && ruleValue.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = if (isEditing) "Save smart folder" else "Generate smart folder"
                    }
            ) {
                Text(if (isEditing) "Save Smart Folder" else "Generate Smart Link")
            }
        }
    }
}
