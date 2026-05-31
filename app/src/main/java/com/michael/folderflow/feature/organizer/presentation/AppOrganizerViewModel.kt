package com.michael.folderflow.feature.organizer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michael.folderflow.core.domain.AppModel
import com.michael.folderflow.core.domain.GetAppsUseCase
import com.michael.folderflow.core.domain.OverrideAppCategoryUseCase
import com.michael.folderflow.core.domain.PlayStoreCategories
import com.michael.folderflow.core.domain.RemoveAppUseCase
import com.michael.folderflow.core.domain.ScanInstalledAppsUseCase
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
    private val scanInstalledAppsUseCase: ScanInstalledAppsUseCase,
    private val overrideAppCategoryUseCase: OverrideAppCategoryUseCase,
    private val removeAppUseCase: RemoveAppUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _sortField = MutableStateFlow("name")
    val sortField: StateFlow<String> = _sortField.asStateFlow()

    private val _sortDirection = MutableStateFlow("ASC")
    val sortDirection: StateFlow<String> = _sortDirection.asStateFlow()

    private val rawApps = getAppsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val listUiState: StateFlow<AppOrganizerUiState> = combine(
        rawApps,
        _searchQuery,
        _selectedCategory,
        _sortField,
        _sortDirection
    ) { apps, query, cat, sortField, sortDirection ->
        val normalizedQuery = query.trim()
        val filtered = apps.filter { app ->
            val matchesQuery = normalizedQuery.isBlank() ||
                app.label.contains(normalizedQuery, ignoreCase = true) ||
                app.packageName.contains(normalizedQuery, ignoreCase = true)
            val matchesCategory = cat == "All" || app.displayCategory.equals(cat, ignoreCase = true)
            matchesQuery && matchesCategory
        }
        val sorted = when (sortField) {
            "package" -> {
                if (sortDirection == "ASC") filtered.sortedBy { it.packageName.lowercase() }
                else filtered.sortedByDescending { it.packageName.lowercase() }
            }
            "category" -> {
                if (sortDirection == "ASC") filtered.sortedBy { it.displayCategory.lowercase() }
                else filtered.sortedByDescending { it.displayCategory.lowercase() }
            }
            "size" -> {
                if (sortDirection == "ASC") filtered.sortedBy { it.sizeBytes }
                else filtered.sortedByDescending { it.sizeBytes }
            }
            else -> {
                if (sortDirection == "ASC") filtered.sortedBy { it.label.lowercase() }
                else filtered.sortedByDescending { it.label.lowercase() }
            }
        }

        // Get unique categories found in the system
        val playCategories = PlayStoreCategories.all.map { it.name }
        val availableCategories = listOf("All") + (playCategories + apps.map { it.displayCategory })
            .distinct()
            .sorted()

        AppOrganizerUiState.Success(sorted, availableCategories)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppOrganizerUiState.Loading
    )

    fun app(packageName: String): Flow<AppModel?> {
        return rawApps.map { apps -> apps.find { it.packageName == packageName } }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSort(field: String, direction: String) {
        _sortField.value = field
        _sortDirection.value = direction
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            scanInstalledAppsUseCase(mockIfLowCount = true)
        }
    }

    fun toggleSort(field: String) {
        if (_sortField.value == field) {
            _sortDirection.value = if (_sortDirection.value == "ASC") "DESC" else "ASC"
        } else {
            _sortField.value = field
            _sortDirection.value = "ASC"
        }
    }

    fun overrideCategory(packageName: String, customCategory: String?) {
        viewModelScope.launch {
            overrideAppCategoryUseCase(packageName, customCategory)
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch {
            removeAppUseCase(packageName)
        }
    }
}
