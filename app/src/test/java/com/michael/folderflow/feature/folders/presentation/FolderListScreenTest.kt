package com.michael.folderflow.feature.folders.presentation

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.michael.folderflow.core.domain.CreateDynamicFolderUseCase
import com.michael.folderflow.core.domain.DeleteFolderUseCase
import com.michael.folderflow.core.domain.FolderModel
import com.michael.folderflow.core.domain.FolderRule
import com.michael.folderflow.core.domain.GetFolderUseCase
import com.michael.folderflow.core.domain.GetFoldersUseCase
import com.michael.folderflow.core.domain.ManageTagsUseCase
import com.michael.folderflow.core.domain.RuleType
import com.michael.folderflow.core.domain.ScanInstalledAppsUseCase
import com.michael.folderflow.core.domain.UpdateFolderUseCase
import com.michael.folderflow.core.domain.UpdateFolderSortUseCase
import com.michael.folderflow.testutil.FakeAppRepository
import com.michael.folderflow.testutil.FakeFolderRepository
import com.michael.folderflow.testutil.FakeTagRepository
import com.michael.folderflow.testutil.MainDispatcherRule
import com.michael.folderflow.ui.theme.FolderFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FolderListScreenTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `smart folder primary action opens sheet and enables valid submit`() {
        val repo = FakeFolderRepository()
        val viewModel = FolderViewModel(
            getFoldersUseCase = GetFoldersUseCase(repo),
            getFolderUseCase = GetFolderUseCase(repo),
            createDynamicFolderUseCase = CreateDynamicFolderUseCase(repo),
            updateFolderUseCase = UpdateFolderUseCase(repo),
            deleteFolderUseCase = DeleteFolderUseCase(repo),
            updateFolderSortUseCase = UpdateFolderSortUseCase(repo),
            scanInstalledAppsUseCase = ScanInstalledAppsUseCase(FakeAppRepository()),
            manageTagsUseCase = ManageTagsUseCase(FakeTagRepository())
        )

        composeTestRule.setContent {
            FolderFlowTheme(darkTheme = false) {
                FolderListScreen(viewModel = viewModel, onNavigateToDetail = {})
            }
        }

        composeTestRule.onNodeWithText("No folders created yet").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Create smart folder").assertHasClickAction().performClick()
        composeTestRule.onNodeWithText("Establish Smart Folder").assertIsDisplayed()
        composeTestRule.onNodeWithText("Generate Smart Link").assertIsNotEnabled()

        composeTestRule.onNodeWithText("Folder Name").performTextInput("Games")
        composeTestRule.onNodeWithText("Folder Icon (Emoji)").performTextInput("G")
        composeTestRule.onNodeWithText("Category Value (e.g. Games, Social)").performTextInput("Games")

        composeTestRule.onNodeWithContentDescription("Generate smart folder")
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun `custom folder edit action opens edit sheet with current folder`() {
        val repo = FakeFolderRepository(
            listOf(
                FolderModel(
                    id = "work",
                    name = "Work",
                    iconEmoji = "W",
                    colorToken = "#3B82F6",
                    isSystem = false,
                    sortOrder = 0,
                    sortField = "name",
                    sortDirection = "ASC",
                    rules = listOf(FolderRule("rule-work", "work", RuleType.TAG, "Work"))
                )
            )
        )
        val viewModel = FolderViewModel(
            getFoldersUseCase = GetFoldersUseCase(repo),
            getFolderUseCase = GetFolderUseCase(repo),
            createDynamicFolderUseCase = CreateDynamicFolderUseCase(repo),
            updateFolderUseCase = UpdateFolderUseCase(repo),
            deleteFolderUseCase = DeleteFolderUseCase(repo),
            updateFolderSortUseCase = UpdateFolderSortUseCase(repo),
            scanInstalledAppsUseCase = ScanInstalledAppsUseCase(FakeAppRepository()),
            manageTagsUseCase = ManageTagsUseCase(FakeTagRepository())
        )

        composeTestRule.setContent {
            FolderFlowTheme(darkTheme = false) {
                FolderListScreen(viewModel = viewModel, onNavigateToDetail = {})
            }
        }

        composeTestRule.onNodeWithContentDescription("Edit folder Work")
            .assertHasClickAction()
            .performClick()

        composeTestRule.onNodeWithText("Edit Smart Folder").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Save smart folder").assertIsEnabled()
    }

    @Test
    fun `custom folder can be deleted from folder card`() {
        val repo = FakeFolderRepository(
            listOf(
                FolderModel(
                    id = "work",
                    name = "Work",
                    iconEmoji = "W",
                    colorToken = "#3B82F6",
                    isSystem = false,
                    sortOrder = 0,
                    sortField = "name",
                    sortDirection = "ASC",
                    rules = listOf(FolderRule("rule-work", "work", RuleType.TAG, "Work"))
                )
            )
        )
        val viewModel = FolderViewModel(
            getFoldersUseCase = GetFoldersUseCase(repo),
            getFolderUseCase = GetFolderUseCase(repo),
            createDynamicFolderUseCase = CreateDynamicFolderUseCase(repo),
            updateFolderUseCase = UpdateFolderUseCase(repo),
            deleteFolderUseCase = DeleteFolderUseCase(repo),
            updateFolderSortUseCase = UpdateFolderSortUseCase(repo),
            scanInstalledAppsUseCase = ScanInstalledAppsUseCase(FakeAppRepository()),
            manageTagsUseCase = ManageTagsUseCase(FakeTagRepository())
        )

        composeTestRule.setContent {
            FolderFlowTheme(darkTheme = false) {
                FolderListScreen(viewModel = viewModel, onNavigateToDetail = {})
            }
        }

        composeTestRule.onNodeWithContentDescription("Delete folder Work")
            .assertHasClickAction()
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            repo.deletedFolders == listOf("work")
        }

        assertEquals(listOf("work"), repo.deletedFolders)
    }
}
