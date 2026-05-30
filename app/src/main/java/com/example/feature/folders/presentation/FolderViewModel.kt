package com.example.feature.folders.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.domain.*
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
    private val updateFolderSortUseCase: UpdateFolderSortUseCase
) : ViewModel() {

    val listUiState: StateFlow<FolderListUiState> = getFoldersUseCase()
        .map { FolderListUiState.Success(it) as FolderListUiState }
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

    fun createFolder(name: String, iconEmoji: String, colorToken: String, rules: List<FolderRule>) {
        viewModelScope.launch {
            createDynamicFolderUseCase(name, iconEmoji, colorToken, rules)
        }
    }

    fun updateSort(folderId: String, sortField: String, sortDirection: String) {
        viewModelScope.launch {
            updateFolderSortUseCase(folderId, sortField, sortDirection)
        }
    }
}
