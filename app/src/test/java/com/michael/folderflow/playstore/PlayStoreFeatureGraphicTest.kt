package com.michael.folderflow.playstore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michael.folderflow.feature.folders.presentation.FolderListScreen
import com.michael.folderflow.ui.theme.FolderFlowTheme
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Category(PlayStoreScreenshotTests::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PlayStoreFeatureGraphicTest {
    @Test
    @Config(qualifiers = "w1024dp-h500dp-mdpi")
    fun feature_graphic() = capturePlayStoreImage("feature-graphic.png") {
        FeatureGraphicContent()
    }
}

@Composable
private fun FeatureGraphicContent() {
    FolderFlowTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF2563EB), Color(0xFF10B981))))
                .padding(horizontal = 64.dp, vertical = 42.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("FolderFlow", color = Color.White, fontWeight = FontWeight.Black, fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Smart app flows, tags, and idle cleanup", color = Color.White.copy(alpha = 0.9f), fontSize = 28.sp)
                }
                Surface(
                    modifier = Modifier
                        .size(width = 310.dp, height = 390.dp)
                        .clip(RoundedCornerShape(32.dp)),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 12.dp
                ) {
                    FolderListScreen(createPlayStoreFolderViewModel(), onNavigateToDetail = {})
                }
            }
        }
    }
}
