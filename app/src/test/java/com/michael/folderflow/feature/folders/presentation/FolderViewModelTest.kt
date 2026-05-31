package com.michael.folderflow.feature.folders.presentation

import com.michael.folderflow.core.domain.CreateDynamicFolderUseCase
import com.michael.folderflow.core.domain.DeleteFolderUseCase
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
import com.michael.folderflow.testutil.UpdatedFolder
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FolderViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `create folder delegates payload to repository`() {
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
        val rule = FolderRule("rule-1", "", RuleType.CATEGORY, "Games")

        viewModel.createFolder("Games", "G", "#3B82F6", listOf(rule))

        val created = repo.createdFolders.single()
        assertEquals("Games", created.name)
        assertEquals("G", created.iconEmoji)
        assertEquals("#3B82F6", created.colorToken)
        assertEquals(rule, created.rules.single())
    }

    @Test
    fun `edit folder delegates updated folder metadata and rule`() = runTest {
        val repo = FakeFolderRepository(
            listOf(
                com.michael.folderflow.core.domain.FolderModel(
                    id = "folder-1",
                    name = "Old",
                    iconEmoji = "O",
                    colorToken = "#3B82F6",
                    isSystem = false,
                    sortOrder = 0,
                    sortField = "name",
                    sortDirection = "ASC",
                    rules = listOf(FolderRule("rule-1", "folder-1", RuleType.CATEGORY, "Games"))
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

        val newRule = FolderRule("", "", RuleType.TAG, "Work")
        viewModel.updateFolder("folder-1", "Work", "W", "#10B981", listOf(newRule))
        advanceUntilIdle()

        assertEquals(
            UpdatedFolder("folder-1", "Work", "W", "#10B981", listOf(newRule)),
            repo.updatedFolders.single()
        )
    }

    @Test
    fun `delete folder delegates to repository`() = runTest {
        val repo = FakeFolderRepository(
            listOf(
                com.michael.folderflow.core.domain.FolderModel(
                    id = "folder-1",
                    name = "Work",
                    iconEmoji = "W",
                    colorToken = "#3B82F6",
                    isSystem = false,
                    sortOrder = 0,
                    sortField = "name",
                    sortDirection = "ASC"
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

        viewModel.deleteFolder("folder-1")
        advanceUntilIdle()

        assertEquals(listOf("folder-1"), repo.deletedFolders)
    }
}
