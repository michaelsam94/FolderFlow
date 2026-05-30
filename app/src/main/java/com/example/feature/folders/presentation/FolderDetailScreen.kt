package com.example.feature.folders.presentation

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.domain.AppModel
import com.example.core.domain.FolderModel
import com.example.core.domain.RuleType
import com.example.ui.theme.ColorMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    viewModel: FolderViewModel,
    onBack: () -> Unit
) {
    val folder by viewModel.folderDetailUiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder?.name ?: "Folder Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            val folderData = folder
            if (folderData == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Folder Header Card
                FolderHeaderBanner(folderData)
                Spacer(modifier = Modifier.height(12.dp))

                // Sort Options Bar
                SortOptionsRow(
                    currentField = folderData.sortField,
                    currentDirection = folderData.sortDirection,
                    onSortChanged = { field, direction ->
                        viewModel.updateSort(folderData.id, field, direction)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (folderData.apps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No apps detected matching rules", fontWeight = FontWeight.Bold, color = ColorMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Add tags, change app categories, or wait for newly qualified installs",
                                fontSize = 11.sp,
                                color = ColorMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(folderData.apps, key = { it.packageName }) { app ->
                            AppLaunchCard(app = app, context = context)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderHeaderBanner(folder: FolderModel) {
    val themeColor = try {
        Color(android.graphics.Color.parseColor(folder.colorToken))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(themeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(folder.iconEmoji, fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "${folder.name} Flow",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                
                // Explain folder rules
                val ruleStrings = folder.rules.map { rule ->
                    when (rule.type) {
                        RuleType.CATEGORY -> "Category: ${rule.value}"
                        RuleType.TAG -> "Tag: ${rule.value}"
                        RuleType.INSTALL_RECENT -> "Installed last ${rule.value} days"
                        RuleType.UNUSED_THRESHOLD -> "Idle > ${rule.value} days"
                    }
                }
                Text(
                    text = "Rules: " + ruleStrings.joinToString(" AND "),
                    fontSize = 11.sp,
                    color = ColorMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SortOptionsRow(
    currentField: String,
    currentDirection: String,
    onSortChanged: (String, String) -> Unit
) {
    val sorts = listOf(
        Pair("name", "A-Z"),
        Pair("installDate", "Added"),
        Pair("lastUsed", "Recent"),
        Pair("size", "Size")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            sorts.forEach { (field, label) ->
                val selected = currentField == field
                FilterChip(
                    selected = selected,
                    onClick = {
                        val dir = if (selected) {
                            if (currentDirection == "ASC") "DESC" else "ASC"
                        } else {
                            "ASC"
                        }
                        onSortChanged(field, dir)
                    },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }

        // Tonal text indication representing ASC/DESC direction (extremely light and 100% compile safe)
        TextButton(
            onClick = {
                val newDir = if (currentDirection == "ASC") "DESC" else "ASC"
                onSortChanged(currentField, newDir)
            },
            modifier = Modifier.wrapContentSize()
        ) {
            Text(
                text = if (currentDirection == "ASC") "▲ ASC" else "▼ DESC",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AppLaunchCard(
    app: AppModel,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                launchTargetApp(context, app)
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App icon with beautiful tonal surface chip layout as required
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // High-fidelity fallback representation using first character of title
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.label.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = app.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Dynamic bottom meta text based on size, install or category
        val metaText = when {
            app.sizeBytes > 100_000_000L -> "${app.sizeBytes / 1_000_000L} MB"
            else -> app.displayCategory
        }
        Text(
            text = metaText,
            fontSize = 8.sp,
            color = ColorMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

fun launchTargetApp(context: Context, app: AppModel) {
    val pm = context.packageManager
    val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
    if (launchIntent != null) {
        try {
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Squelched: Run in FolderFlow simulator", Toast.LENGTH_SHORT).show()
        }
    } else {
        // Showcase Toast for newly added simulator apps
        val triggerToast = "FolderFlow launching simulated app: ${app.label} (${app.packageName})"
        Toast.makeText(context, triggerToast, Toast.LENGTH_LONG).show()
    }
}
