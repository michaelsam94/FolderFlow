package com.michael.folderflow.feature.organizer.presentation

import com.michael.folderflow.core.domain.AppModel
import com.michael.folderflow.core.domain.GetAppsUseCase
import com.michael.folderflow.core.domain.OverrideAppCategoryUseCase
import com.michael.folderflow.core.domain.PlayStoreCategories
import com.michael.folderflow.core.domain.RemoveAppUseCase
import com.michael.folderflow.core.domain.ScanInstalledAppsUseCase
import com.michael.folderflow.testutil.FakeAppRepository
import com.michael.folderflow.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppOrganizerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `filters apps by query and selected category`() = runTest {
        val repo = FakeAppRepository(
            listOf(
                app("com.cash", "Cash App", "Finance"),
                app("com.chat", "Chat Space", "Social"),
                app("com.calc", "Calculator", "Utilities")
            )
        )
        val viewModel = AppOrganizerViewModel(
            getAppsUseCase = GetAppsUseCase(repo),
            scanInstalledAppsUseCase = ScanInstalledAppsUseCase(repo),
            overrideAppCategoryUseCase = OverrideAppCategoryUseCase(repo),
            removeAppUseCase = RemoveAppUseCase(repo)
        )

        viewModel.setSearchQuery("cash")
        viewModel.setSelectedCategory("Finance")

        val state = viewModel.listUiState.filterIsInstance<AppOrganizerUiState.Success>().first()
        assertEquals(listOf("Cash App"), state.apps.map { it.label })
    }

    @Test
    fun `trims query and sorts filtered apps`() = runTest {
        val repo = FakeAppRepository(
            listOf(
                app("com.alpha", "Alpha Notes", "Tools", sizeBytes = 300L),
                app("com.beta", "Beta Notes", "Tools", sizeBytes = 100L),
                app("com.gamma", "Gamma Chat", "Social", sizeBytes = 200L)
            )
        )
        val viewModel = AppOrganizerViewModel(
            getAppsUseCase = GetAppsUseCase(repo),
            scanInstalledAppsUseCase = ScanInstalledAppsUseCase(repo),
            overrideAppCategoryUseCase = OverrideAppCategoryUseCase(repo),
            removeAppUseCase = RemoveAppUseCase(repo)
        )

        viewModel.setSearchQuery(" notes ")
        viewModel.setSelectedCategory("Tools")
        viewModel.setSort("size", "ASC")

        val state = viewModel.listUiState.filterIsInstance<AppOrganizerUiState.Success>().first()
        assertEquals(listOf("Beta Notes", "Alpha Notes"), state.apps.map { it.label })
    }

    @Test
    fun `category override is delegated to repository`() = runTest {
        val repo = FakeAppRepository(listOf(app("com.chat", "Chat Space", "Social")))
        val viewModel = appOrganizerViewModel(repo)

        viewModel.overrideCategory("com.chat", "Tools")

        assertEquals(listOf("com.chat" to "Tools"), repo.overrides)
    }

    @Test
    fun `remove app deletes it from organizer list`() = runTest {
        val repo = FakeAppRepository(
            listOf(
                app("com.chat", "Chat Space", "Social"),
                app("com.notes", "Notes", "Tools")
            )
        )
        val viewModel = appOrganizerViewModel(repo)

        viewModel.removeApp("com.chat")

        val state = viewModel.listUiState.filterIsInstance<AppOrganizerUiState.Success>().first()
        assertEquals(listOf("Notes"), state.apps.map { it.label })
    }


    @Test
    fun `category chips include all Play categories even when no app is present`() = runTest {
        val repo = FakeAppRepository(listOf(app("com.cash", "Cash App", "Finance")))
        val viewModel = appOrganizerViewModel(repo)

        val state = viewModel.listUiState.filterIsInstance<AppOrganizerUiState.Success>().first()

        assertEquals("All", state.categories.first())
        assertEquals(true, "Health and Fitness" in state.categories)
        assertEquals(true, "Role Playing" in state.categories)
        assertEquals(true, PlayStoreCategories.all.all { it.name in state.categories })
    }

    @Test
    fun `refresh installed apps delegates scan without pruning mock fallback data`() = runTest {
        val repo = FakeAppRepository()
        val viewModel = appOrganizerViewModel(repo)

        viewModel.refreshInstalledApps()
        advanceUntilIdle()

        assertEquals(listOf(true), repo.scanRequests)
    }

    private fun appOrganizerViewModel(repo: FakeAppRepository): AppOrganizerViewModel {
        return AppOrganizerViewModel(
            getAppsUseCase = GetAppsUseCase(repo),
            scanInstalledAppsUseCase = ScanInstalledAppsUseCase(repo),
            overrideAppCategoryUseCase = OverrideAppCategoryUseCase(repo),
            removeAppUseCase = RemoveAppUseCase(repo)
        )
    }

    private fun app(
        packageName: String,
        label: String,
        category: String,
        sizeBytes: Long = 1_024L
    ) = AppModel(
        packageName = packageName,
        label = label,
        category = category,
        installDate = 1L,
        lastUsed = 1L,
        sizeBytes = sizeBytes
    )
}
