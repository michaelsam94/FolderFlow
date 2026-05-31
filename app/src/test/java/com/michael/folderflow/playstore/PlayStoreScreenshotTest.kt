package com.michael.folderflow.playstore

import com.michael.folderflow.feature.folders.presentation.FolderListScreen
import com.michael.folderflow.feature.organizer.presentation.AppOrganizerScreen
import com.michael.folderflow.feature.tags.presentation.TagsScreen
import com.michael.folderflow.feature.unused.presentation.UnusedAppsScreen
import com.michael.folderflow.ui.theme.FolderFlowTheme
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val PHONE = "w360dp-h640dp-xxhdpi"
private const val TABLET = "w800dp-h1280dp-xhdpi"

@RunWith(RobolectricTestRunner::class)
@Category(PlayStoreScreenshotTests::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PlayStoreScreenshotTest {
    @Test
    @Config(qualifiers = PHONE)
    fun phone_01_live_folders() = capturePlayStoreImage("phone/01_live_folders.png") {
        FolderFlowTheme(darkTheme = false) {
            FolderListScreen(createPlayStoreFolderViewModel(), onNavigateToDetail = {})
        }
    }

    @Test
    @Config(qualifiers = PHONE)
    fun phone_02_app_index() = capturePlayStoreImage("phone/02_app_index.png") {
        FolderFlowTheme(darkTheme = false) {
            AppOrganizerScreen(createPlayStoreOrganizerViewModel())
        }
    }

    @Test
    @Config(qualifiers = PHONE)
    fun phone_03_tags() = capturePlayStoreImage("phone/03_tags.png") {
        FolderFlowTheme(darkTheme = false) {
            TagsScreen(createPlayStoreTagsViewModel())
        }
    }

    @Test
    @Config(qualifiers = PHONE)
    fun phone_04_idle_scanner() = capturePlayStoreImage("phone/04_idle_scanner.png") {
        FolderFlowTheme(darkTheme = false) {
            UnusedAppsScreen(createPlayStoreUnusedViewModel())
        }
    }

    @Test
    @Config(qualifiers = TABLET)
    fun tablet_01_live_folders() = capturePlayStoreImage("tablet/01_live_folders.png") {
        FolderFlowTheme(darkTheme = false) {
            FolderListScreen(createPlayStoreFolderViewModel(), onNavigateToDetail = {})
        }
    }

    @Test
    @Config(qualifiers = TABLET)
    fun tablet_02_app_index() = capturePlayStoreImage("tablet/02_app_index.png") {
        FolderFlowTheme(darkTheme = false) {
            AppOrganizerScreen(createPlayStoreOrganizerViewModel())
        }
    }
}
