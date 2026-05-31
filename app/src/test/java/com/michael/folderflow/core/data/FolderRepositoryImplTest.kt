package com.michael.folderflow.core.data

import com.michael.folderflow.core.domain.AppModel
import com.michael.folderflow.core.domain.AppRepository
import com.michael.folderflow.core.domain.PlayStoreCategories
import com.michael.folderflow.core.domain.TagModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderRepositoryImplTest {
    @Test
    fun `seeds a system folder and category rule for every Play category`() = runTest {
        val folderDao = InMemoryFolderDao()
        val repository = FolderRepositoryImpl(folderDao, EmptyAppRepository())

        repository.seedDefaultSystemFolders()

        val categoryFolders = folderDao.folders.filter { it.folderId.startsWith("system_") }
            .filterNot { it.folderId in setOf("system_new", "system_unused") }
        assertEquals(PlayStoreCategories.all.map { it.name }, categoryFolders.map { it.name })
        assertEquals(PlayStoreCategories.all.size + 2, folderDao.folders.size)
        assertTrue(folderDao.rules.any { it.ruleValue == "Health and Fitness" })
        assertTrue(folderDao.rules.any { it.ruleValue == "Role Playing" })
    }

    @Test
    fun `tag folder matches tagged apps with whitespace-insensitive rule value`() = runTest {
        val folderDao = InMemoryFolderDao()
        val repository = FolderRepositoryImpl(
            folderDao,
            StaticAppRepository(
                listOf(
                    AppModel(
                        packageName = "com.notes",
                        label = "Notes",
                        category = "Tools",
                        installDate = 1L,
                        lastUsed = 1L,
                        sizeBytes = 1_024L,
                        tags = listOf(TagModel("tag-work", "Work", "#3B82F6"))
                    )
                )
            )
        )
        folderDao.insertFolder(
            FolderEntity(
                folderId = "folder-work",
                name = "Work",
                iconEmoji = "W",
                colorToken = "#3B82F6",
                isSystem = false,
                sortOrder = 0,
                sortField = "name",
                sortDirection = "ASC"
            )
        )
        folderDao.insertRule(
            FolderRuleEntity(
                ruleId = "rule-work",
                folderId = "folder-work",
                ruleType = "TAG",
                ruleValue = " work "
            )
        )

        val folder = repository.getFolder("folder-work").first()

        assertEquals(listOf("Notes"), folder?.apps?.map { it.label })
    }

    private class InMemoryFolderDao : FolderDao {
        val folders = mutableListOf<FolderEntity>()
        val rules = mutableListOf<FolderRuleEntity>()
        private val foldersFlow = MutableStateFlow<List<FolderEntity>>(emptyList())
        private val rulesFlow = MutableStateFlow<List<FolderRuleEntity>>(emptyList())

        override fun getAllFoldersFlow(): Flow<List<FolderEntity>> = foldersFlow

        override suspend fun getAllFolders(): List<FolderEntity> = folders

        override suspend fun getFolderById(folderId: String): FolderEntity? {
            return folders.find { it.folderId == folderId }
        }

        override suspend fun insertFolder(folder: FolderEntity) {
            folders.removeAll { it.folderId == folder.folderId }
            folders += folder
            foldersFlow.value = folders.toList()
        }

        override suspend fun deleteFolder(folderId: String) {
            folders.removeAll { it.folderId == folderId }
            foldersFlow.value = folders.toList()
        }

        override fun getAllRulesFlow(): Flow<List<FolderRuleEntity>> = rulesFlow

        override suspend fun getRulesForFolder(folderId: String): List<FolderRuleEntity> {
            return rules.filter { it.folderId == folderId }
        }

        override suspend fun insertRules(rules: List<FolderRuleEntity>) {
            rules.forEach { insertRule(it) }
        }

        override suspend fun insertRule(rule: FolderRuleEntity) {
            rules.removeAll { it.ruleId == rule.ruleId }
            rules += rule
            rulesFlow.value = rules.toList()
        }

        override suspend fun deleteRule(ruleId: String) {
            rules.removeAll { it.ruleId == ruleId }
            rulesFlow.value = rules.toList()
        }

        override suspend fun deleteRulesForFolder(folderId: String) {
            rules.removeAll { it.folderId == folderId }
            rulesFlow.value = rules.toList()
        }
    }

    private class EmptyAppRepository : AppRepository {
        override fun getAllApps(): Flow<List<AppModel>> = flowOf(emptyList())
        override fun getApp(packageName: String): Flow<AppModel?> = flowOf(null)
        override suspend fun scanAndSyncApps(mockIfLowCount: Boolean) = Unit
        override suspend fun overrideCategory(packageName: String, customCategory: String?) = Unit
        override suspend fun removeApp(packageName: String) = Unit
        override suspend fun seedMockApps() = Unit
    }

    private class StaticAppRepository(private val apps: List<AppModel>) : AppRepository {
        override fun getAllApps(): Flow<List<AppModel>> = flowOf(apps)
        override fun getApp(packageName: String): Flow<AppModel?> = flowOf(apps.find { it.packageName == packageName })
        override suspend fun scanAndSyncApps(mockIfLowCount: Boolean) = Unit
        override suspend fun overrideCategory(packageName: String, customCategory: String?) = Unit
        override suspend fun removeApp(packageName: String) = Unit
        override suspend fun seedMockApps() = Unit
    }
}
