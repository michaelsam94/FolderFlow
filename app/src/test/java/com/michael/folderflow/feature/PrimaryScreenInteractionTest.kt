package com.michael.folderflow.feature

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.michael.folderflow.core.domain.AppModel
import com.michael.folderflow.core.domain.CreateDynamicFolderUseCase
import com.michael.folderflow.core.domain.DeleteFolderUseCase
import com.michael.folderflow.core.domain.FolderModel
import com.michael.folderflow.core.domain.FolderRule
import com.michael.folderflow.core.domain.GetAppsUseCase
import com.michael.folderflow.core.domain.GetFolderUseCase
import com.michael.folderflow.core.domain.GetFoldersUseCase
import com.michael.folderflow.core.domain.GetUnusedAppsUseCase
import com.michael.folderflow.core.domain.ManageTagsUseCase
import com.michael.folderflow.core.domain.OverrideAppCategoryUseCase
import com.michael.folderflow.core.domain.RemoveAppUseCase
import com.michael.folderflow.core.domain.RuleType
import com.michael.folderflow.core.domain.ScanInstalledAppsUseCase
import com.michael.folderflow.core.domain.TagModel
import com.michael.folderflow.core.domain.UpdateFolderUseCase
import com.michael.folderflow.core.domain.UpdateFolderSortUseCase
import com.michael.folderflow.feature.folders.presentation.FolderDetailScreen
import com.michael.folderflow.feature.folders.presentation.FolderDetailRoute
import com.michael.folderflow.feature.folders.presentation.FolderViewModel
import com.michael.folderflow.feature.organizer.presentation.AppOrganizerScreen
import com.michael.folderflow.feature.organizer.presentation.AppOrganizerViewModel
import com.michael.folderflow.feature.organizer.presentation.AppSystemActions
import com.michael.folderflow.feature.organizer.presentation.AppDetailScreen
import com.michael.folderflow.feature.tags.presentation.TagsScreen
import com.michael.folderflow.feature.tags.presentation.TagsViewModel
import com.michael.folderflow.feature.unused.presentation.UnusedAppsScreen
import com.michael.folderflow.feature.unused.presentation.UnusedAppsViewModel
import com.michael.folderflow.testutil.FakeAppRepository
import com.michael.folderflow.testutil.FakeFolderRepository
import com.michael.folderflow.testutil.FakeTagRepository
import com.michael.folderflow.testutil.MainDispatcherRule
import com.michael.folderflow.ui.theme.FolderFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrimaryScreenInteractionTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `folder detail actions are visible clickable and update sorting`() {
        val repo = FakeFolderRepository(
            listOf(
                folder(
                    id = "productivity",
                    name = "Productivity",
                    apps = listOf(app("com.notes", "Notes", "Tools"))
                )
            )
        )
        val viewModel = folderViewModel(repo)
        viewModel.selectFolder("productivity")
        var wentBack = false

        composeTestRule.setContent {
            FolderFlowTheme(darkTheme = false) {
                FolderDetailScreen(viewModel = viewModel, onBack = { wentBack = true })
            }
        }

        composeTestRule.onNodeWithText("Productivity Flow").assertIsDisplayed()
        composeTestRule.onNodeWithText("A-Z").assertHasClickAction().performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) { repo.sortUpdates.isNotEmpty() }
        assertEquals(Triple("productivity", "name", "DESC"), repo.sortUpdates.single())

        composeTestRule.onNodeWithContentDescription("Launch Notes").assertHasClickAction().assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertHasClickAction().performClick()
        assertEquals(true, wentBack)
    }

    @Test
    fun `folder detail route selects folder from navigation argument`() {
        val repo = FakeFolderRepository(
            listOf(
                folder(
                    id = "productivity",
                    name = "Productivity",
                    apps = listOf(app("com.notes", "Notes", "Tools"))
                )
            )
        )
        val viewModel = folderViewModel(repo)

        composeTestRule.setContent {
            FolderFlowTheme(darkTheme = false) {
                FolderDetailRoute(
                    viewModel = viewModel,
                    folderId = "productivity",
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Productivity Flow").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Launch Notes").assertIsDisplayed()
    }

    @Test
    fun `organizer detail category override is clickable`() {
        val apps = listOf(
            app("com.cash", "Cash App", "Finance"),
            app("com.notes", "Notes", "Tools")
        )
        val repo = FakeAppRepository(apps)
        val viewModel = appOrganizerViewModel(repo)

        composeTestRule.setContent {
            var selectedApp by remember { mutableStateOf<AppModel?>(null) }
            FolderFlowTheme(darkTheme = false) {
                selectedApp?.let { app ->
                    AppDetailScreen(
                        app = app,
                        actions = RecordingAppSystemActions(),
                        onBack = { selectedApp = null },
                        onOverrideCategory = { category ->
                            viewModel.overrideCategory(app.packageName, category)
                        }
                    )
                } ?: AppOrganizerScreen(
                    viewModel = viewModel,
                    onOpenAppDetails = { packageName ->
                        selectedApp = apps.first { it.packageName == packageName }
                    }
                )
            }
        }

        composeTestRule.onNodeWithText("Search package or app name...").performTextInput("cash")
        composeTestRule.onNodeWithText("Cash App").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Open details for Cash App")
            .assertHasClickAction()
            .performClick()
        composeTestRule.onNodeWithText("Override classification")
            .performScrollTo()
            .assertHasClickAction()
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText("Override Classification").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Override Classification").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Override category Art and Design")
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) { repo.overrides.isNotEmpty() }
        assertEquals(listOf("com.cash" to "Art and Design"), repo.overrides)
    }

    @Test
    fun `organizer app tap opens app detail instead of category override`() {
        val repo = FakeAppRepository(
            listOf(
                app("com.cash", "Cash App", "Finance"),
                app("com.notes", "Notes", "Tools")
            )
        )
        val viewModel = appOrganizerViewModel(repo)
        var openedPackageName: String? = null

        composeTestRule.setContent {
            FolderFlowTheme(darkTheme = false) {
                AppOrganizerScreen(
                    viewModel = viewModel,
                    onOpenAppDetails = { openedPackageName = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Search package or app name...").performTextInput("cash")
        composeTestRule.onNodeWithText("Cash App").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Open details for Cash App")
            .assertHasClickAction()
            .performClick()

        assertEquals("com.cash", openedPackageName)
        assertEquals(0, composeTestRule.onAllNodesWithText("Override Classification").fetchSemanticsNodes().size)
    }

    @Test
    fun `app detail screen shows resource stats and app management actions`() {
        val app = app("com.cash", "Cash App", "Finance")
        val actions = RecordingAppSystemActions()

        composeTestRule.setContent {
            FolderFlowTheme(darkTheme = false) {
                AppDetailScreen(
                    app = app,
                    actions = actions,
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Cash App").assertIsDisplayed()
        composeTestRule.onNodeWithText("Resource usage").assertIsDisplayed()
        composeTestRule.onNodeWithText("Storage").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("Memory").fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("CPU").fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("Battery").fetchSemanticsNodes().size)

        composeTestRule.onNodeWithText("Open app settings")
            .performScrollTo()
            .assertHasClickAction()
            .performClick()
        composeTestRule.onNodeWithText("Storage & cache")
            .performScrollTo()
            .assertHasClickAction()
            .performClick()
        composeTestRule.onNodeWithText("Uninstall")
            .performScrollTo()
            .assertHasClickAction()
            .performClick()

        assertEquals(listOf("settings:com.cash", "storage:com.cash", "uninstall:com.cash"), actions.calls)
    }

    @Test
    fun `android app actions launch uninstall intent for installed packages`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val actions = com.michael.folderflow.feature.organizer.presentation.AndroidAppSystemActions(context)

        actions.requestUninstall(context.packageName)

        val startedIntent = shadowOf(context as Application).nextStartedActivity
        assertEquals(android.content.Intent.ACTION_DELETE, startedIntent.action)
        assertEquals("package:${context.packageName}", startedIntent.data.toString())
    }

    @Test
    fun `android app actions explain when a mock package cannot be uninstalled`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val actions = com.michael.folderflow.feature.organizer.presentation.AndroidAppSystemActions(context)

        actions.requestUninstall("com.folderflow.mock.missing")

        assertEquals(null, shadowOf(context as Application).nextStartedActivity)
        assertEquals(
            "This app is not installed on this device.",
            ShadowToast.getTextOfLatestToast()
        )
    }

    @Test
    fun `tags screen creates deletes and bulk toggles app tags`() {
        val appRepo = FakeAppRepository(listOf(app("com.notes", "Notes", "Tools")))
        val tagRepo = FakeTagRepository(listOf(TagModel("tag-work", "work", "#3B82F6")))
        val viewModel = TagsViewModel(
            GetAppsUseCase(appRepo),
            ManageTagsUseCase(tagRepo),
            ScanInstalledAppsUseCase(appRepo)
        )

        composeTestRule.setContent {
            FolderFlowTheme(darkTheme = false) {
                TagsScreen(viewModel)
            }
        }

        composeTestRule.onNodeWithText("e.g. work, kids, travel").performTextInput("travel")
        composeTestRule.onNodeWithContentDescription("Select color #10B981").assertHasClickAction().performClick()
        composeTestRule.onNodeWithText("Create").assertIsEnabled().performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            (viewModel.uiState.value as? com.michael.folderflow.feature.tags.presentation.TagsUiState.Success)
                ?.tags
                ?.any { it.label == "travel" } == true
        }
        composeTestRule.onAllNodesWithContentDescription("Bulk Tag Apps")[0]
            .assertHasClickAction()
            .performClick()
        composeTestRule.onNodeWithText("Bulk Tagging: work").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Toggle tag work for Notes")
            .assertHasClickAction()
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            "com.notes" to "tag-work" in tagRepo.appTagLinks
        }

        composeTestRule.onNodeWithText("Apply & Sync").performClick()
        composeTestRule.onAllNodesWithContentDescription("Delete Tag")[0]
            .assertHasClickAction()
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            (viewModel.uiState.value as? com.michael.folderflow.feature.tags.presentation.TagsUiState.Success)
                ?.tags
                ?.none { it.id == "tag-work" } == true
        }
    }

    @Test
    fun `idle scanner exposes notification threshold and uninstall actions`() {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        val repo = FakeAppRepository(
            listOf(
                app("com.stale", "Old Notes", "Tools", lastUsed = now - 95 * day),
                app("com.active", "Today Notes", "Tools", lastUsed = now - 3 * day)
            )
        )
        val viewModel = UnusedAppsViewModel(GetUnusedAppsUseCase(repo), ScanInstalledAppsUseCase(repo))

        composeTestRule.setContent {
            FolderFlowTheme(darkTheme = false) {
                UnusedAppsScreen(viewModel)
            }
        }

        composeTestRule.onNodeWithContentDescription("Toggle dormancy reminders")
            .assertHasClickAction()
            .performClick()
        composeTestRule.onNodeWithText("60 Days").assertHasClickAction().performClick()
        assertEquals(60, viewModel.thresholdDays.value)
        composeTestRule.onNodeWithText("Old Notes").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Uninstall app").assertHasClickAction().assertIsDisplayed()
    }

    private fun folderViewModel(repo: FakeFolderRepository) = FolderViewModel(
        getFoldersUseCase = GetFoldersUseCase(repo),
        getFolderUseCase = GetFolderUseCase(repo),
        createDynamicFolderUseCase = CreateDynamicFolderUseCase(repo),
        updateFolderUseCase = UpdateFolderUseCase(repo),
        deleteFolderUseCase = DeleteFolderUseCase(repo),
        updateFolderSortUseCase = UpdateFolderSortUseCase(repo),
        scanInstalledAppsUseCase = ScanInstalledAppsUseCase(FakeAppRepository()),
        manageTagsUseCase = ManageTagsUseCase(FakeTagRepository())
    )

    private fun appOrganizerViewModel(repo: FakeAppRepository) = AppOrganizerViewModel(
        getAppsUseCase = GetAppsUseCase(repo),
        scanInstalledAppsUseCase = ScanInstalledAppsUseCase(repo),
        overrideAppCategoryUseCase = OverrideAppCategoryUseCase(repo),
        removeAppUseCase = RemoveAppUseCase(repo)
    )

    private fun folder(
        id: String,
        name: String,
        apps: List<AppModel> = emptyList()
    ) = FolderModel(
        id = id,
        name = name,
        iconEmoji = "P",
        colorToken = "#3B82F6",
        isSystem = false,
        sortOrder = 0,
        sortField = "name",
        sortDirection = "ASC",
        rules = listOf(FolderRule("rule-$id", id, RuleType.CATEGORY, "Tools")),
        apps = apps
    )

    private fun app(
        packageName: String,
        label: String,
        category: String,
        lastUsed: Long = System.currentTimeMillis()
    ) = AppModel(
        packageName = packageName,
        label = label,
        category = category,
        installDate = lastUsed,
        lastUsed = lastUsed,
        sizeBytes = 1_024L
    )

    private class RecordingAppSystemActions : AppSystemActions {
        val calls = mutableListOf<String>()

        override fun openSettings(packageName: String) {
            calls += "settings:$packageName"
        }

        override fun openStorageSettings(packageName: String) {
            calls += "storage:$packageName"
        }

        override fun requestUninstall(packageName: String) {
            calls += "uninstall:$packageName"
        }

        override fun isInstalled(packageName: String): Boolean = true
    }
}
