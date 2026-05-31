package com.michael.folderflow.testutil

import com.michael.folderflow.core.domain.AppModel
import com.michael.folderflow.core.domain.AppRepository
import com.michael.folderflow.core.domain.FolderModel
import com.michael.folderflow.core.domain.FolderRepository
import com.michael.folderflow.core.domain.FolderRule
import com.michael.folderflow.core.domain.RuleType
import com.michael.folderflow.core.domain.TagModel
import com.michael.folderflow.core.domain.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAppRepository(apps: List<AppModel> = emptyList()) : AppRepository {
    private val appsFlow = MutableStateFlow(apps)
    val overrides = mutableListOf<Pair<String, String?>>()
    val scanRequests = mutableListOf<Boolean>()

    override fun getAllApps(): Flow<List<AppModel>> = appsFlow

    override fun getApp(packageName: String): Flow<AppModel?> {
        return appsFlow.map { apps -> apps.find { it.packageName == packageName } }
    }

    override suspend fun scanAndSyncApps(mockIfLowCount: Boolean) {
        scanRequests += mockIfLowCount
    }

    override suspend fun overrideCategory(packageName: String, customCategory: String?) {
        overrides += packageName to customCategory
        appsFlow.value = appsFlow.value.map { app ->
            if (app.packageName == packageName) app.copy(customCategory = customCategory) else app
        }
    }

    override suspend fun removeApp(packageName: String) {
        appsFlow.value = appsFlow.value.filterNot { it.packageName == packageName }
    }

    override suspend fun seedMockApps() = Unit
}

class FakeFolderRepository(folders: List<FolderModel> = emptyList()) : FolderRepository {
    private val foldersFlow = MutableStateFlow(folders)
    val createdFolders = mutableListOf<CreatedFolder>()
    val updatedFolders = mutableListOf<UpdatedFolder>()
    val deletedFolders = mutableListOf<String>()
    val sortUpdates = mutableListOf<Triple<String, String, String>>()

    override fun getAllFolders(): Flow<List<FolderModel>> = foldersFlow

    override fun getFolder(folderId: String): Flow<FolderModel?> {
        return foldersFlow.map { folders -> folders.find { it.id == folderId } }
    }

    override suspend fun createFolder(
        name: String,
        iconEmoji: String,
        colorToken: String,
        rules: List<FolderRule>
    ): String {
        val id = "created-${createdFolders.size + 1}"
        createdFolders += CreatedFolder(name, iconEmoji, colorToken, rules)
        foldersFlow.value = foldersFlow.value + FolderModel(
            id = id,
            name = name,
            iconEmoji = iconEmoji,
            colorToken = colorToken,
            isSystem = false,
            sortOrder = foldersFlow.value.size,
            sortField = "name",
            sortDirection = "ASC",
            rules = rules
        )
        return id
    }

    override suspend fun updateFolder(
        folderId: String,
        name: String,
        iconEmoji: String,
        colorToken: String,
        rules: List<FolderRule>
    ) {
        updatedFolders += UpdatedFolder(folderId, name, iconEmoji, colorToken, rules)
        foldersFlow.value = foldersFlow.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(
                    name = name,
                    iconEmoji = iconEmoji,
                    colorToken = colorToken,
                    rules = rules.map { rule ->
                        rule.copy(
                            id = rule.id.ifBlank { "rule-$folderId" },
                            folderId = folderId
                        )
                    }
                )
            } else {
                folder
            }
        }
    }

    override suspend fun deleteFolder(folderId: String) {
        deletedFolders += folderId
        foldersFlow.value = foldersFlow.value.filterNot { it.id == folderId }
    }

    override suspend fun updateFolderSort(folderId: String, sortField: String, sortDirection: String) {
        sortUpdates += Triple(folderId, sortField, sortDirection)
    }

    override suspend fun seedDefaultSystemFolders() = Unit
}

class FakeTagRepository(tags: List<TagModel> = emptyList()) : TagRepository {
    private val tagsFlow = MutableStateFlow(tags)
    val appTagLinks = mutableSetOf<Pair<String, String>>()

    override fun getAllTags(): Flow<List<TagModel>> = tagsFlow

    override suspend fun createTag(label: String, colorHex: String): String {
        val id = "tag-${tagsFlow.value.size + 1}"
        tagsFlow.value = tagsFlow.value + TagModel(id, label, colorHex)
        return id
    }

    override suspend fun deleteTag(tagId: String) {
        tagsFlow.value = tagsFlow.value.filterNot { it.id == tagId }
    }

    override suspend fun addTagToApp(packageName: String, tagId: String) {
        appTagLinks += packageName to tagId
    }

    override suspend fun removeTagFromApp(packageName: String, tagId: String) {
        appTagLinks -= packageName to tagId
    }
}

data class CreatedFolder(
    val name: String,
    val iconEmoji: String,
    val colorToken: String,
    val rules: List<FolderRule>
)

data class UpdatedFolder(
    val folderId: String,
    val name: String,
    val iconEmoji: String,
    val colorToken: String,
    val rules: List<FolderRule>
)
