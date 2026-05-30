package com.example.feature.unused.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.domain.AppModel
import com.example.core.domain.GetUnusedAppsUseCase
import kotlinx.coroutines.flow.*

sealed interface UnusedAppsUiState {
    object Loading : UnusedAppsUiState
    data class Success(val unusedApps: List<AppModel>) : UnusedAppsUiState
}

class UnusedAppsViewModel(
    private val getUnusedAppsUseCase: GetUnusedAppsUseCase
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
}
