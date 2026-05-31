package com.michael.folderflow.feature.tags.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.michael.folderflow.core.domain.AppModel
import com.michael.folderflow.core.domain.TagModel
import com.michael.folderflow.feature.folders.presentation.SectionHeader
import com.michael.folderflow.ui.theme.DarkPrimary
import com.michael.folderflow.ui.theme.ColorMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    viewModel: TagsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var newTagLabel by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#3B82F6") } // Defaults to Blue
    var showCustomColorPicker by remember { mutableStateOf(false) }
    var activeTagForBulk by remember { mutableStateOf<TagModel?>(null) }
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

    val presetColors = listOf("#3B82F6", "#EF4444", "#10B981", "#F59E0B", "#8B5CF6", "#EC4899", "#06B6D4")
    val selectedColor = colorFromHex(selectedColorHex)

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(
                title = "Custom Tags",
                subtitle = "Create custom taxonomy labels for apps"
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Create tag block inside a neat surface card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Register Custom Tag", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTagLabel,
                            onValueChange = { newTagLabel = it },
                            placeholder = { Text("e.g. work, kids, travel") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                if (newTagLabel.isNotBlank()) {
                                    viewModel.createTag(newTagLabel, normalizeHexColor(selectedColorHex))
                                    newTagLabel = ""
                                }
                            },
                            enabled = newTagLabel.isNotBlank() && isValidHexColor(selectedColorHex),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Create")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(selectedColor)
                                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                                .semantics { contentDescription = "Selected tag color $selectedColorHex" }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Selected $selectedColorHex",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Color swatches selector
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetColors) { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 30.dp else 26.dp)
                                    .clip(CircleShape)
                                    .semantics { contentDescription = "Select color $hex" }
                                    .background(color)
                                    .border(
                                        BorderStroke(
                                            if (isSelected) 3.dp else 1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                        ),
                                        CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(16.dp)
                                    )
                                }
                            }
                        }
                        item {
                            OutlinedButton(
                                onClick = { showCustomColorPicker = true },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Custom", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // Master tags list
            when (val state = uiState) {
                is TagsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is TagsUiState.Success -> {
                    val tags = state.tags
                    val apps = state.apps
                    
                    if (tags.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No custom tags created yet", fontWeight = FontWeight.Bold, color = ColorMuted)
                        }
                    } else {
                        Text(
                            text = "ACTIVE LABELS (${tags.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = ColorMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(tags, key = { it.id }) { tag ->
                                TagRowItem(
                                    tag = tag,
                                    apps = apps,
                                    onBulkTagClick = { activeTagForBulk = tag },
                                    onDelete = { viewModel.deleteTag(tag.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bulk Tagging BottomSheet
    activeTagForBulk?.let { tag ->
        val state = uiState as? TagsUiState.Success
        if (state != null) {
            BulkTagBottomSheet(
                tag = tag,
                apps = state.apps,
                onDismiss = { activeTagForBulk = null },
                onToggleAppTag = { app, isChecked ->
                    if (isChecked) {
                        viewModel.addTagToApp(app.packageName, tag.id)
                    } else {
                        viewModel.removeTagFromApp(app.packageName, tag.id)
                    }
                }
            )
        }
    }

    if (showCustomColorPicker) {
        CustomColorDialog(
            initialColorHex = selectedColorHex,
            onDismiss = { showCustomColorPicker = false },
            onColorSelected = { colorHex ->
                selectedColorHex = colorHex
                showCustomColorPicker = false
            }
        )
    }
}

@Composable
fun CustomColorDialog(
    initialColorHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    var colorInput by remember(initialColorHex) { mutableStateOf(initialColorHex) }
    val normalized = normalizeHexColor(colorInput)
    val valid = isValidHexColor(colorInput)
    val paletteColors = remember {
        listOf(
            "#EF4444", "#F97316", "#F59E0B", "#EAB308", "#84CC16", "#22C55E",
            "#10B981", "#14B8A6", "#06B6D4", "#0EA5E9", "#3B82F6", "#6366F1",
            "#8B5CF6", "#A855F7", "#D946EF", "#EC4899", "#F43F5E", "#64748B",
            "#111827", "#6B7280", "#92400E", "#166534", "#155E75", "#581C87"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Color Palette") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (valid) colorFromHex(normalized) else Color.Transparent)
                        .border(BorderStroke(2.dp, MaterialTheme.colorScheme.outline), CircleShape)
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp)
                ) {
                    items(paletteColors) { hex ->
                        val selected = normalized.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colorFromHex(hex))
                                .border(
                                    BorderStroke(
                                        if (selected) 3.dp else 1.dp,
                                        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                    ),
                                    CircleShape
                                )
                                .semantics { contentDescription = "Select custom color $hex" }
                                .clickable { colorInput = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = colorInput,
                    onValueChange = { colorInput = it.take(7) },
                    label = { Text("Hex color") },
                    placeholder = { Text("#22C55E") },
                    singleLine = true,
                    isError = colorInput.isNotBlank() && !valid,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Tap a swatch or enter #RRGGBB",
                    fontSize = 11.sp,
                    color = ColorMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onColorSelected(normalized) },
                enabled = valid
            ) {
                Text("Use Color")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TagRowItem(
    tag: TagModel,
    apps: List<AppModel>,
    onBulkTagClick: () -> Unit,
    onDelete: () -> Unit
) {
    val tagColor = colorFromHex(tag.colorHex)
    val associatedAppsCount = apps.count { app -> app.tags.any { it.id == tag.id } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tag Indicator Circle
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(tagColor)
        )
        
        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tag.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$associatedAppsCount associated apps",
                fontSize = 10.sp,
                color = ColorMuted
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Bulk tag button
        IconButton(onClick = onBulkTagClick) {
            Icon(Icons.Default.List, contentDescription = "Bulk Tag Apps", tint = MaterialTheme.colorScheme.primary)
        }

        // Delete tag button
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "Delete Tag", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkTagBottomSheet(
    tag: TagModel,
    apps: List<AppModel>,
    onDismiss: () -> Unit,
    onToggleAppTag: (AppModel, Boolean) -> Unit
) {
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
            val tagColor = colorFromHex(tag.colorHex)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(tagColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bulk Tagging: ${tag.label}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Add or remove the tag label on multiple apps instantly",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                items(apps) { app ->
                    val isTagged = app.tags.any { it.id == tag.id }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Toggle tag ${tag.label} for ${app.label}" }
                            .clickable { onToggleAppTag(app, !isTagged) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(app.label.take(1).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(app.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(app.packageName, fontSize = 9.sp, color = ColorMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Checkbox(
                            checked = isTagged,
                            onCheckedChange = { isChecked -> onToggleAppTag(app, isChecked) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply & Sync")
            }
        }
    }
}

private fun isValidHexColor(value: String): Boolean {
    return Regex("^#?[0-9A-Fa-f]{6}$").matches(value.trim())
}

private fun normalizeHexColor(value: String): String {
    val trimmed = value.trim()
    return if (trimmed.startsWith("#")) trimmed.uppercase() else "#${trimmed.uppercase()}"
}

private fun colorFromHex(value: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(normalizeHexColor(value)))
    } catch (e: Exception) {
        Color(0xFF3B82F6)
    }
}
