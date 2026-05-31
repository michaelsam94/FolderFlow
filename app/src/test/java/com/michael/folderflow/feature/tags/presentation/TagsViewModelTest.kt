package com.michael.folderflow.feature.tags.presentation

import com.michael.folderflow.core.domain.AppModel
import com.michael.folderflow.core.domain.GetAppsUseCase
import com.michael.folderflow.core.domain.ManageTagsUseCase
import com.michael.folderflow.core.domain.ScanInstalledAppsUseCase
import com.michael.folderflow.core.domain.TagModel
import com.michael.folderflow.testutil.FakeAppRepository
import com.michael.folderflow.testutil.FakeTagRepository
import com.michael.folderflow.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TagsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `create delete and app tag mutations are delegated`() = runTest {
        val appRepo = FakeAppRepository(listOf(app()))
        val tagRepo = FakeTagRepository(listOf(TagModel("tag-work", "work", "#3B82F6")))
        val viewModel = TagsViewModel(
            GetAppsUseCase(appRepo),
            ManageTagsUseCase(tagRepo),
            ScanInstalledAppsUseCase(appRepo)
        )

        viewModel.createTag("travel", "#10B981")
        viewModel.addTagToApp("com.notes", "tag-work")
        viewModel.removeTagFromApp("com.notes", "tag-work")
        viewModel.deleteTag("tag-work")

        val state = viewModel.uiState.filterIsInstance<TagsUiState.Success>().first()
        assertEquals(listOf("travel"), state.tags.map { it.label })
        assertEquals(emptySet<Pair<String, String>>(), tagRepo.appTagLinks)
    }

    private fun app() = AppModel(
        packageName = "com.notes",
        label = "Notes",
        category = "Tools",
        installDate = 1L,
        lastUsed = 1L,
        sizeBytes = 1_024L
    )
}
