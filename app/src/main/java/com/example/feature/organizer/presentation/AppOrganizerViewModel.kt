package com.example.feature.organizer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.domain.AppModel
import com.example.core.domain.GetAppsUseCase
import com.example.core.domain.OverrideAppCategoryUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AppOrganizerUiState {
    object Loading : AppOrganizerUiState
    data class Success(
        val apps: List<AppModel>,
        val categories: List<String>
    ) : AppOrganizerUiState
}

class AppOrganizerViewModel(
    private val getAppsUseCase: GetAppsUseCase,
    private val overrideAppCategoryUseCase: OverrideAppCategoryUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val rawApps = getAppsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val listUiState: StateFlow<AppOrganizerUiState> = combine(
        rawApps,
        _searchQuery,
        _selectedCategory
    ) { apps, query, cat ->
        val filtered = apps.filter { app ->
            val matchesQuery = app.label.contains(query, ignoreCase = true) || 
                             app.packageName.contains(query, ignoreCase = true)
            val matchesCategory = cat == "All" || app.displayCategory.equals(cat, ignoreCase = true)
            matchesQuery && matchesCategory
        }

        // Get unique categories found in the system
        val availableCategories = listOf("All") + apps.map { it.displayCategory }.distinct().sorted()

        AppOrganizerUiState.Success(filtered, availableCategories)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppOrganizerUiState.Loading
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun overrideCategory(packageName: String, customCategory: String?) {
        viewModelScope.launch {
            overrideAppCategoryUseCase(packageName, customCategory)
        }
    }
}
