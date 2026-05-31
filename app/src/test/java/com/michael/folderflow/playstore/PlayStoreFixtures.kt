package com.michael.folderflow.playstore

import com.michael.folderflow.core.domain.AppModel
import com.michael.folderflow.core.domain.FolderModel
import com.michael.folderflow.core.domain.FolderRule
import com.michael.folderflow.core.domain.GetAppsUseCase
import com.michael.folderflow.core.domain.GetFolderUseCase
import com.michael.folderflow.core.domain.GetFoldersUseCase
import com.michael.folderflow.core.domain.GetUnusedAppsUseCase
import com.michael.folderflow.core.domain.ManageTagsUseCase
import com.michael.folderflow.core.domain.RuleType
import com.michael.folderflow.core.domain.ScanInstalledAppsUseCase
import com.michael.folderflow.core.domain.TagModel
import com.michael.folderflow.core.domain.CreateDynamicFolderUseCase
import com.michael.folderflow.core.domain.DeleteFolderUseCase
import com.michael.folderflow.core.domain.UpdateFolderSortUseCase
import com.michael.folderflow.core.domain.UpdateFolderUseCase
import com.michael.folderflow.feature.folders.presentation.FolderViewModel
import com.michael.folderflow.feature.organizer.presentation.AppOrganizerViewModel
import com.michael.folderflow.core.domain.OverrideAppCategoryUseCase
import com.michael.folderflow.core.domain.RemoveAppUseCase
import com.michael.folderflow.feature.tags.presentation.TagsViewModel
import com.michael.folderflow.feature.unused.presentation.UnusedAppsViewModel
import com.michael.folderflow.testutil.FakeAppRepository
import com.michael.folderflow.testutil.FakeFolderRepository
import com.michael.folderflow.testutil.FakeTagRepository

private const val day = 24 * 60 * 60 * 1000L

fun playStoreApps(): List<AppModel> {
    val now = System.currentTimeMillis()
    val workTag = TagModel("tag-work", "Work", "#3B82F6")
    val homeTag = TagModel("tag-home", "Home", "#10B981")
    return listOf(
        app("com.docs", "Docs", "Productivity", now - 2 * day, now - 1 * day, listOf(workTag)),
        app("com.mail", "Mail", "Communications", now - 20 * day, now - 3 * day, listOf(workTag)),
        app("com.camera", "Camera", "Photography", now - 90 * day, now - 4 * day, emptyList()),
        app("com.recipes", "Recipes", "Food and Drink", now - 130 * day, now - 72 * day, listOf(homeTag)),
        app("com.arcade", "Arcade", "Games", now - 180 * day, now - 95 * day, emptyList()),
        app("com.bank", "Bank", "Finance", now - 210 * day, now - 12 * day, emptyList())
    )
}

fun playStoreTags() = listOf(
    TagModel("tag-work", "Work", "#3B82F6"),
    TagModel("tag-home", "Home", "#10B981"),
    TagModel("tag-travel", "Travel", "#F59E0B")
)

fun playStoreFolders(apps: List<AppModel>): List<FolderModel> {
    return listOf(
        folder("work", "Work Flow", "W", "#3B82F6", RuleType.TAG, "Work", apps.filter { app -> app.tags.any { it.label == "Work" } }),
        folder("recent", "New Apps", "N", "#F59E0B", RuleType.INSTALL_RECENT, "30", apps.filter { it.installDate > System.currentTimeMillis() - 30 * day }),
        folder("idle", "Unused Apps", "I", "#EF4444", RuleType.UNUSED_THRESHOLD, "60", apps.filter { it.lastUsed < System.currentTimeMillis() - 60 * day }),
        folder("finance", "Finance", "F", "#10B981", RuleType.CATEGORY, "Finance", apps.filter { it.category == "Finance" })
    )
}

fun createPlayStoreFolderViewModel(): FolderViewModel {
    val apps = playStoreApps()
    val folderRepo = FakeFolderRepository(playStoreFolders(apps))
    val appRepo = FakeAppRepository(apps)
    val tagRepo = FakeTagRepository(playStoreTags())
    return FolderViewModel(
        getFoldersUseCase = GetFoldersUseCase(folderRepo),
        getFolderUseCase = GetFolderUseCase(folderRepo),
        createDynamicFolderUseCase = CreateDynamicFolderUseCase(folderRepo),
        updateFolderUseCase = UpdateFolderUseCase(folderRepo),
        deleteFolderUseCase = DeleteFolderUseCase(folderRepo),
        updateFolderSortUseCase = UpdateFolderSortUseCase(folderRepo),
        scanInstalledAppsUseCase = ScanInstalledAppsUseCase(appRepo),
        manageTagsUseCase = ManageTagsUseCase(tagRepo)
    )
}

fun createPlayStoreOrganizerViewModel(): AppOrganizerViewModel {
    val appRepo = FakeAppRepository(playStoreApps())
    return AppOrganizerViewModel(
        getAppsUseCase = GetAppsUseCase(appRepo),
        scanInstalledAppsUseCase = ScanInstalledAppsUseCase(appRepo),
        overrideAppCategoryUseCase = OverrideAppCategoryUseCase(appRepo),
        removeAppUseCase = RemoveAppUseCase(appRepo)
    )
}

fun createPlayStoreTagsViewModel(): TagsViewModel {
    val appRepo = FakeAppRepository(playStoreApps())
    val tagRepo = FakeTagRepository(playStoreTags())
    return TagsViewModel(
        getAppsUseCase = GetAppsUseCase(appRepo),
        manageTagsUseCase = ManageTagsUseCase(tagRepo),
        scanInstalledAppsUseCase = ScanInstalledAppsUseCase(appRepo)
    )
}

fun createPlayStoreUnusedViewModel(): UnusedAppsViewModel {
    val appRepo = FakeAppRepository(playStoreApps())
    return UnusedAppsViewModel(
        getUnusedAppsUseCase = GetUnusedAppsUseCase(appRepo),
        scanInstalledAppsUseCase = ScanInstalledAppsUseCase(appRepo)
    )
}

private fun app(
    packageName: String,
    label: String,
    category: String,
    installDate: Long,
    lastUsed: Long,
    tags: List<TagModel>
) = AppModel(packageName, label, category, installDate = installDate, lastUsed = lastUsed, sizeBytes = 42_000_000L, tags = tags)

private fun folder(
    id: String,
    name: String,
    icon: String,
    color: String,
    ruleType: RuleType,
    ruleValue: String,
    apps: List<AppModel>
) = FolderModel(
    id = id,
    name = name,
    iconEmoji = icon,
    colorToken = color,
    isSystem = id in setOf("recent", "idle", "finance"),
    sortOrder = 0,
    sortField = "name",
    sortDirection = "ASC",
    rules = listOf(FolderRule("rule-$id", id, ruleType, ruleValue)),
    apps = apps
)

