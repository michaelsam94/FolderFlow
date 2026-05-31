package com.michael.folderflow.feature.unused.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michael.folderflow.core.domain.AppModel
import com.michael.folderflow.core.domain.GetUnusedAppsUseCase
import com.michael.folderflow.core.domain.ScanInstalledAppsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UnusedAppsUiState {
    object Loading : UnusedAppsUiState
    data class Success(val unusedApps: List<AppModel>) : UnusedAppsUiState
}

class UnusedAppsViewModel(
    private val getUnusedAppsUseCase: GetUnusedAppsUseCase,
    private val scanInstalledAppsUseCase: ScanInstalledAppsUseCase
) : ViewModel() {

    private val _thresholdDays = MutableStateFlow(30)
    val thresholdDays: StateFlow<Int> = _thresholdDays.asStateFlow()

    val uiState: StateFlow<UnusedAppsUiState> = _thresholdDays
        .flatMapLatest { days ->
            getUnusedAppsUseCase(days).map { list ->
                UnusedAppsUiState.Success(list) as UnusedAppsUiState
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UnusedAppsUiState.Loading
        )

    fun setThresholdDays(days: Int) {
        _thresholdDays.value = days
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            scanInstalledAppsUseCase(mockIfLowCount = true)
        }
    }
}
