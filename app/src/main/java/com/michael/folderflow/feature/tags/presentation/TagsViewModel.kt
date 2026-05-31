package com.michael.folderflow.feature.tags.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michael.folderflow.core.domain.AppModel
import com.michael.folderflow.core.domain.GetAppsUseCase
import com.michael.folderflow.core.domain.ManageTagsUseCase
import com.michael.folderflow.core.domain.ScanInstalledAppsUseCase
import com.michael.folderflow.core.domain.TagModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface TagsUiState {
    object Loading : TagsUiState
    data class Success(
        val tags: List<TagModel>,
        val apps: List<AppModel>
    ) : TagsUiState
}

class TagsViewModel(
    private val getAppsUseCase: GetAppsUseCase,
    private val manageTagsUseCase: ManageTagsUseCase,
    private val scanInstalledAppsUseCase: ScanInstalledAppsUseCase
) : ViewModel() {

    val uiState: StateFlow<TagsUiState> = combine(
        manageTagsUseCase.getTags(),
        getAppsUseCase()
    ) { tags, apps ->
        TagsUiState.Success(tags, apps)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TagsUiState.Loading
    )

    fun createTag(label: String, colorHex: String) {
        viewModelScope.launch {
            manageTagsUseCase.createTag(label, colorHex)
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            scanInstalledAppsUseCase(mockIfLowCount = true)
        }
    }

    fun deleteTag(tagId: String) {
        viewModelScope.launch {
            manageTagsUseCase.deleteTag(tagId)
        }
    }

    fun addTagToApp(packageName: String, tagId: String) {
        viewModelScope.launch {
            manageTagsUseCase.addTagToApp(packageName, tagId)
        }
    }

    fun removeTagFromApp(packageName: String, tagId: String) {
        viewModelScope.launch {
            manageTagsUseCase.removeTagFromApp(packageName, tagId)
        }
    }
}
