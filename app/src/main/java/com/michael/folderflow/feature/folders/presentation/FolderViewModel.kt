package com.michael.folderflow.feature.folders.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michael.folderflow.core.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface FolderListUiState {
    object Loading : FolderListUiState
    data class Success(val folders: List<FolderModel>) : FolderListUiState
    data class Error(val message: String) : FolderListUiState
}

class FolderViewModel(
    private val getFoldersUseCase: GetFoldersUseCase,
    private val getFolderUseCase: GetFolderUseCase,
    private val createDynamicFolderUseCase: CreateDynamicFolderUseCase,
    private val updateFolderUseCase: UpdateFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
    private val updateFolderSortUseCase: UpdateFolderSortUseCase,
    private val scanInstalledAppsUseCase: ScanInstalledAppsUseCase,
    private val manageTagsUseCase: ManageTagsUseCase
) : ViewModel() {

    val listUiState: StateFlow<FolderListUiState> = getFoldersUseCase()
        .map { folders ->
            val visibleFolders = folders.filter { folder ->
                !folder.isSystem ||
                    folder.id == "system_new" ||
                    folder.id == "system_unused" ||
                    folder.apps.isNotEmpty()
            }
            FolderListUiState.Success(visibleFolders) as FolderListUiState
        }
        .catch { emit(FolderListUiState.Error(it.localizedMessage ?: "Unknown Error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FolderListUiState.Loading
        )

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    
    val folderDetailUiState: StateFlow<FolderModel?> = _selectedFolderId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else getFolderUseCase(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun selectFolder(folderId: String?) {
        _selectedFolderId.value = folderId
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            scanInstalledAppsUseCase(mockIfLowCount = true)
        }
    }

    fun createFolder(name: String, iconEmoji: String, colorToken: String, rules: List<FolderRule>) {
        viewModelScope.launch {
            val normalizedRules = rules.normalized()
            ensureTagRulesExist(normalizedRules, colorToken)
            createDynamicFolderUseCase(name.trim(), iconEmoji.trim(), colorToken, normalizedRules)
        }
    }

    fun updateFolder(
        folderId: String,
        name: String,
        iconEmoji: String,
        colorToken: String,
        rules: List<FolderRule>
    ) {
        viewModelScope.launch {
            val normalizedRules = rules.normalized()
            ensureTagRulesExist(normalizedRules, colorToken)
            updateFolderUseCase(folderId, name.trim(), iconEmoji.trim(), colorToken, normalizedRules)
        }
    }

    fun updateSort(folderId: String, sortField: String, sortDirection: String) {
        viewModelScope.launch {
            updateFolderSortUseCase(folderId, sortField, sortDirection)
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            if (_selectedFolderId.value == folderId) {
                _selectedFolderId.value = null
            }
            deleteFolderUseCase(folderId)
        }
    }

    private suspend fun ensureTagRulesExist(rules: List<FolderRule>, colorToken: String) {
        rules
            .filter { it.type == RuleType.TAG }
            .map { it.value.trim() }
            .filter { it.isNotBlank() }
            .forEach { tagLabel ->
                val existingTags = manageTagsUseCase.getTags().first()
                if (existingTags.none { it.label.equals(tagLabel, ignoreCase = true) }) {
                    manageTagsUseCase.createTag(tagLabel, colorToken)
                }
            }
    }

    private fun List<FolderRule>.normalized(): List<FolderRule> {
        return map { rule -> rule.copy(value = rule.value.trim()) }
    }
}
