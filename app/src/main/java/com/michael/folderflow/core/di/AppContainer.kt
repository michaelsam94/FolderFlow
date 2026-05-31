package com.michael.folderflow.core.di

import android.content.Context
import com.michael.folderflow.core.data.*
import com.michael.folderflow.core.domain.*

interface AppContainer {
    val appRepository: AppRepository
    val folderRepository: FolderRepository
    val tagRepository: TagRepository
    
    // Use Cases
    val getFoldersUseCase: GetFoldersUseCase
    val getFolderUseCase: GetFolderUseCase
    val getAppsUseCase: GetAppsUseCase
    val scanInstalledAppsUseCase: ScanInstalledAppsUseCase
    val getUnusedAppsUseCase: GetUnusedAppsUseCase
    val createDynamicFolderUseCase: CreateDynamicFolderUseCase
    val updateFolderUseCase: UpdateFolderUseCase
    val deleteFolderUseCase: DeleteFolderUseCase
    val updateFolderSortUseCase: UpdateFolderSortUseCase
    val overrideAppCategoryUseCase: OverrideAppCategoryUseCase
    val removeAppUseCase: RemoveAppUseCase
    val manageTagsUseCase: ManageTagsUseCase
}

class AppContainerImpl(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val appRepository: AppRepository by lazy {
        AppRepositoryImpl(context, database.appDao(), database.tagDao())
    }

    override val folderRepository: FolderRepository by lazy {
        FolderRepositoryImpl(database.folderDao(), appRepository)
    }

    override val tagRepository: TagRepository by lazy {
        TagRepositoryImpl(database.tagDao())
    }

    // Use Case Instances
    override val getFoldersUseCase: GetFoldersUseCase by lazy {
        GetFoldersUseCase(folderRepository)
    }

    override val getFolderUseCase: GetFolderUseCase by lazy {
        GetFolderUseCase(folderRepository)
    }

    override val getAppsUseCase: GetAppsUseCase by lazy {
        GetAppsUseCase(appRepository)
    }

    override val scanInstalledAppsUseCase: ScanInstalledAppsUseCase by lazy {
        ScanInstalledAppsUseCase(appRepository)
    }

    override val getUnusedAppsUseCase: GetUnusedAppsUseCase by lazy {
        GetUnusedAppsUseCase(appRepository)
    }

    override val createDynamicFolderUseCase: CreateDynamicFolderUseCase by lazy {
        CreateDynamicFolderUseCase(folderRepository)
    }

    override val updateFolderUseCase: UpdateFolderUseCase by lazy {
        UpdateFolderUseCase(folderRepository)
    }

    override val deleteFolderUseCase: DeleteFolderUseCase by lazy {
        DeleteFolderUseCase(folderRepository)
    }

    override val updateFolderSortUseCase: UpdateFolderSortUseCase by lazy {
        UpdateFolderSortUseCase(folderRepository)
    }

    override val overrideAppCategoryUseCase: OverrideAppCategoryUseCase by lazy {
        OverrideAppCategoryUseCase(appRepository)
    }

    override val removeAppUseCase: RemoveAppUseCase by lazy {
        RemoveAppUseCase(appRepository)
    }

    override val manageTagsUseCase: ManageTagsUseCase by lazy {
        ManageTagsUseCase(tagRepository)
    }
}
