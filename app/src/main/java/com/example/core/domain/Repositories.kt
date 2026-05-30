package com.example.core.domain

import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getAllApps(): Flow<List<AppModel>>
    fun getApp(packageName: String): Flow<AppModel?>
    suspend fun scanAndSyncApps(mockIfLowCount: Boolean = true)
    suspend fun overrideCategory(packageName: String, customCategory: String?)
    suspend fun seedMockApps() // Seeding option to showcase the app with rich datasets
}

interface FolderRepository {
    fun getAllFolders(): Flow<List<FolderModel>>
    fun getFolder(folderId: String): Flow<FolderModel?>
    suspend fun createFolder(
        name: String,
        iconEmoji: String,
        colorToken: String,
        rules: List<FolderRule>
    ): String
    suspend fun deleteFolder(folderId: String)
    suspend fun updateFolderSort(folderId: String, sortField: String, sortDirection: String)
    suspend fun seedDefaultSystemFolders()
}

interface TagRepository {
    fun getAllTags(): Flow<List<TagModel>>
    suspend fun createTag(label: String, colorHex: String): String
    suspend fun deleteTag(tagId: String)
    suspend fun addTagToApp(packageName: String, tagId: String)
    suspend fun removeTagFromApp(packageName: String, tagId: String)
}
