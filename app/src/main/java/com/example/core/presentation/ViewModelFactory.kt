package com.example.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.FolderFlowApplication
import com.example.feature.folders.presentation.FolderViewModel
import com.example.feature.organizer.presentation.AppOrganizerViewModel
import com.example.feature.tags.presentation.TagsViewModel
import com.example.feature.unused.presentation.UnusedAppsViewModel

class ViewModelFactory(private val application: FolderFlowApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val container = application.container
        return when {
            modelClass.isAssignableFrom(FolderViewModel::class.java) -> {
                FolderViewModel(
                    container.getFoldersUseCase,
                    container.getFolderUseCase,
                    container.createDynamicFolderUseCase,
                    container.updateFolderSortUseCase
                ) as T
            }
            modelClass.isAssignableFrom(AppOrganizerViewModel::class.java) -> {
                AppOrganizerViewModel(
                    container.getAppsUseCase,
                    container.overrideAppCategoryUseCase
                ) as T
            }
            modelClass.isAssignableFrom(TagsViewModel::class.java) -> {
                TagsViewModel(
                    container.getAppsUseCase,
                    container.manageTagsUseCase
                ) as T
            }
            modelClass.isAssignableFrom(UnusedAppsViewModel::class.java) -> {
                UnusedAppsViewModel(
                    container.getUnusedAppsUseCase
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
