package com.michael.folderflow.core.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "app_entities")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val category: String,
    val customCategory: String? = null,
    val installDate: Long,
    val lastUsed: Long,
    val sizeBytes: Long,
    val iconUri: String? = null,
    val usageScore: Float = 0f
)

@Entity(tableName = "folder_entities")
data class FolderEntity(
    @PrimaryKey val folderId: String,
    val name: String,
    val iconEmoji: String,
    val colorToken: String,
    val isSystem: Boolean,
    val sortOrder: Int,
    val sortField: String, // "name", "install_date", "last_used", "size"
    val sortDirection: String // "ASC", "DESC"
)

@Entity(
    tableName = "folder_rule_entities",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["folderId"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["folderId"])]
)
data class FolderRuleEntity(
    @PrimaryKey val ruleId: String,
    val folderId: String,
    val ruleType: String, // "CATEGORY", "TAG", "INSTALL_RECENT", "UNUSED_THRESHOLD"
    val ruleValue: String
)

@Entity(tableName = "tag_entities")
data class TagEntity(
    @PrimaryKey val tagId: String,
    val label: String,
    val colorHex: String
)

@Entity(
    tableName = "app_tag_cross_refs",
    primaryKeys = ["packageName", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["packageName"],
            childColumns = ["packageName"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["tagId"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["packageName"]), Index(value = ["tagId"])]
)
data class AppTagCrossRef(
    val packageName: String,
    val tagId: String
)
