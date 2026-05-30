package com.example.feature.organizer.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
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
import com.example.feature.folders.presentation.SectionHeader
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.ColorMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOrganizerScreen(
    viewModel: AppOrganizerViewModel
) {
    val listState by viewModel.listUiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var activeAppForOverride by remember { mutableStateOf<AppModel?>(null) }

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
                    val categories = listOf("All", "Social", "Finance", "Games", "Tools", "Shopping", "Health", "Media", "Utilities")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { viewModel.setSelectedCategory(cat) },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
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
                                    onClick = { activeAppForOverride = app }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    activeAppForOverride?.let { app ->
        EditCategoryBottomSheet(
            app = app,
            onDismiss = { activeAppForOverride = null },
            onOverride = { cat ->
                viewModel.overrideCategory(app.packageName, cat)
                activeAppForOverride = null
            }
        )
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
        Icon(Icons.Default.ArrowForward, contentDescription = "Edit Category", tint = ColorMuted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoryBottomSheet(
    app: AppModel,
    onDismiss: () -> Unit,
    onOverride: (String?) -> Unit
) {
    val categories = listOf("Social", "Finance", "Games", "Tools", "Shopping", "Health", "Media", "Utilities")

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
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.chunked(2).forEach { rowCats ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowCats.forEach { cat ->
                            val isActive = app.displayCategory == cat
                            OutlinedButton(
                                onClick = { onOverride(cat) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                                ),
                                border = ButtonDefaults.outlinedButtonBorder(true).run {
                                    if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    else this
                                },
                                modifier = Modifier.weight(1f)
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
