package com.michael.folderflow.feature.unused.presentation

import com.michael.folderflow.core.domain.AppModel
import com.michael.folderflow.core.domain.GetUnusedAppsUseCase
import com.michael.folderflow.core.domain.ScanInstalledAppsUseCase
import com.michael.folderflow.testutil.FakeAppRepository
import com.michael.folderflow.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UnusedAppsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `threshold changes refresh the stale app list`() = runTest {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        val repo = FakeAppRepository(
            listOf(
                app("com.month", "Month Old", now - 45 * day),
                app("com.quarter", "Quarter Old", now - 95 * day)
            )
        )
        val viewModel = UnusedAppsViewModel(GetUnusedAppsUseCase(repo), ScanInstalledAppsUseCase(repo))

        viewModel.setThresholdDays(90)

        val state = viewModel.uiState.filterIsInstance<UnusedAppsUiState.Success>().first()
        assertEquals(listOf("com.quarter"), state.unusedApps.map { it.packageName })
    }

    @Test
    fun `refresh installed apps delegates scan`() = runTest {
        val repo = FakeAppRepository()
        val viewModel = UnusedAppsViewModel(GetUnusedAppsUseCase(repo), ScanInstalledAppsUseCase(repo))

        viewModel.refreshInstalledApps()
        advanceUntilIdle()

        assertEquals(listOf(true), repo.scanRequests)
    }

    private fun app(packageName: String, label: String, lastUsed: Long) = AppModel(
        packageName = packageName,
        label = label,
        category = "Tools",
        installDate = lastUsed,
        lastUsed = lastUsed,
        sizeBytes = 1_024L
    )
}
