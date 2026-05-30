package com.example.feature.tags.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.domain.AppModel
import com.example.core.domain.TagModel
import com.example.feature.folders.presentation.SectionHeader
import com.example.ui.theme.DarkPrimary
import com.example.ui.theme.ColorMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    viewModel: TagsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var newTagLabel by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#3B82F6") } // Defaults to Blue
    var activeTagForBulk by remember { mutableStateOf<TagModel?>(null) }

    val presetColors = listOf("#3B82F6", "#EF4444", "#10B981", "#F59E0B", "#8B5CF6", "#EC4899", "#06B6D4")

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
                                    viewModel.createTag(newTagLabel, selectedColorHex)
                                    newTagLabel = ""
                                }
                            },
                            enabled = newTagLabel.isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Create")
                        }
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
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColorHex = hex }
                                    .run {
                                        if (isSelected) {
                                            this.background(color, CircleShape)
                                        } else {
                                            this
                                        }
                                    }
                            )
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
}

@Composable
fun TagRowItem(
    tag: TagModel,
    apps: List<AppModel>,
    onBulkTagClick: () -> Unit,
    onDelete: () -> Unit
) {
    val tagColor = Color(android.graphics.Color.parseColor(tag.colorHex))
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
            val tagColor = Color(android.graphics.Color.parseColor(tag.colorHex))

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
