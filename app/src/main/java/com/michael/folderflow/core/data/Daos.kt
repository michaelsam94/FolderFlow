package com.michael.folderflow.core.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM app_entities ORDER BY label ASC")
    fun getAllAppsFlow(): Flow<List<AppEntity>>

    @Query("SELECT * FROM app_entities")
    suspend fun getAllApps(): List<AppEntity>

    @Query("SELECT * FROM app_entities WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackage(packageName: String): AppEntity?

    @Upsert
    suspend fun insertApps(apps: List<AppEntity>)

    @Upsert
    suspend fun insertApp(app: AppEntity)

    @Query("DELETE FROM app_entities WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)

    @Query("DELETE FROM app_entities")
    suspend fun clearAllApps()
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folder_entities ORDER BY sortOrder ASC")
    fun getAllFoldersFlow(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folder_entities ORDER BY sortOrder ASC")
    suspend fun getAllFolders(): List<FolderEntity>

    @Query("SELECT * FROM folder_entities WHERE folderId = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: String): FolderEntity?

    @Upsert
    suspend fun insertFolder(folder: FolderEntity)

    @Query("DELETE FROM folder_entities WHERE folderId = :folderId")
    suspend fun deleteFolder(folderId: String)

    // Folder Rules
    @Query("SELECT * FROM folder_rule_entities")
    fun getAllRulesFlow(): Flow<List<FolderRuleEntity>>

    @Query("SELECT * FROM folder_rule_entities WHERE folderId = :folderId")
    suspend fun getRulesForFolder(folderId: String): List<FolderRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<FolderRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: FolderRuleEntity)

    @Query("DELETE FROM folder_rule_entities WHERE ruleId = :ruleId")
    suspend fun deleteRule(ruleId: String)

    @Query("DELETE FROM folder_rule_entities WHERE folderId = :folderId")
    suspend fun deleteRulesForFolder(folderId: String)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tag_entities ORDER BY label ASC")
    fun getAllTagsFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tag_entities ORDER BY label ASC")
    suspend fun getAllTags(): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Query("DELETE FROM tag_entities WHERE tagId = :tagId")
    suspend fun deleteTag(tagId: String)

    // Cross reference links
    @Query("SELECT * FROM app_tag_cross_refs")
    fun getAllAppTagCrossRefsFlow(): Flow<List<AppTagCrossRef>>

    @Query("SELECT * FROM app_tag_cross_refs")
    suspend fun getAllAppTagCrossRefs(): List<AppTagCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: AppTagCrossRef)

    @Query("DELETE FROM app_tag_cross_refs WHERE packageName = :packageName AND tagId = :tagId")
    suspend fun deleteCrossRef(packageName: String, tagId: String)

    @Query("DELETE FROM app_tag_cross_refs WHERE packageName = :packageName")
    suspend fun deleteCrossRefsForApp(packageName: String)
}
