package com.michael.folderflow.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.michael.folderflow.FolderFlowApplication
import com.michael.folderflow.feature.folders.presentation.FolderViewModel
import com.michael.folderflow.feature.organizer.presentation.AppOrganizerViewModel
import com.michael.folderflow.feature.tags.presentation.TagsViewModel
import com.michael.folderflow.feature.unused.presentation.UnusedAppsViewModel

class ViewModelFactory(private val application: FolderFlowApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val container = application.container
        return when {
            modelClass.isAssignableFrom(FolderViewModel::class.java) -> {
                FolderViewModel(
                    container.getFoldersUseCase,
                    container.getFolderUseCase,
                    container.createDynamicFolderUseCase,
                    container.updateFolderUseCase,
                    container.deleteFolderUseCase,
                    container.updateFolderSortUseCase,
                    container.scanInstalledAppsUseCase,
                    container.manageTagsUseCase
                ) as T
            }
            modelClass.isAssignableFrom(AppOrganizerViewModel::class.java) -> {
                AppOrganizerViewModel(
                    container.getAppsUseCase,
                    container.scanInstalledAppsUseCase,
                    container.overrideAppCategoryUseCase,
                    container.removeAppUseCase
                ) as T
            }
            modelClass.isAssignableFrom(TagsViewModel::class.java) -> {
                TagsViewModel(
                    container.getAppsUseCase,
                    container.manageTagsUseCase,
                    container.scanInstalledAppsUseCase
                ) as T
            }
            modelClass.isAssignableFrom(UnusedAppsViewModel::class.java) -> {
                UnusedAppsViewModel(
                    container.getUnusedAppsUseCase,
                    container.scanInstalledAppsUseCase
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
