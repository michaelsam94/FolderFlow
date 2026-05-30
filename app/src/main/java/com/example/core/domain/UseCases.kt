package com.example.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetFoldersUseCase(private val folderRepository: FolderRepository) {
    operator fun invoke(): Flow<List<FolderModel>> = folderRepository.getAllFolders()
}

class GetFolderUseCase(private val folderRepository: FolderRepository) {
    operator fun invoke(folderId: String): Flow<FolderModel?> = folderRepository.getFolder(folderId)
}

class GetAppsUseCase(private val appRepository: AppRepository) {
    operator fun invoke(): Flow<List<AppModel>> = appRepository.getAllApps()
}

class GetUnusedAppsUseCase(private val appRepository: AppRepository) {
    operator fun invoke(thresholdDays: Int): Flow<List<AppModel>> {
        return appRepository.getAllApps().map { apps ->
            val limitMillis = System.currentTimeMillis() - (thresholdDays * 24 * 60 * 60 * 1000L)
            apps.filter { it.lastUsed < limitMillis }
        }
    }
}

class CreateDynamicFolderUseCase(private val folderRepository: FolderRepository) {
    suspend operator fun invoke(
        name: String,
        iconEmoji: String,
        colorToken: String,
        rules: List<FolderRule>
    ): String {
        return folderRepository.createFolder(name, iconEmoji, colorToken, rules)
    }
}

class UpdateFolderSortUseCase(private val folderRepository: FolderRepository) {
    suspend operator fun invoke(folderId: String, sortField: String, sortDirection: String) {
        folderRepository.updateFolderSort(folderId, sortField, sortDirection)
    }
}

class OverrideAppCategoryUseCase(private val appRepository: AppRepository) {
    suspend operator fun invoke(packageName: String, customCategory: String?) {
        appRepository.overrideCategory(packageName, customCategory)
    }
}

class ManageTagsUseCase(private val tagRepository: TagRepository) {
    fun getTags(): Flow<List<TagModel>> = tagRepository.getAllTags()
    
    suspend fun createTag(label: String, colorHex: String): String {
        return tagRepository.createTag(label, colorHex)
    }

    suspend fun deleteTag(tagId: String) {
        tagRepository.deleteTag(tagId)
    }

    suspend fun addTagToApp(packageName: String, tagId: String) {
        tagRepository.addTagToApp(packageName, tagId)
    }

    suspend fun removeTagFromApp(packageName: String, tagId: String) {
        tagRepository.removeTagFromApp(packageName, tagId)
    }
}
