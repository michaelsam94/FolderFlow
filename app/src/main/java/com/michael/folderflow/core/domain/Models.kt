package com.michael.folderflow.core.domain

enum class RuleType {
    CATEGORY, TAG, INSTALL_RECENT, UNUSED_THRESHOLD
}

data class AppModel(
    val packageName: String,
    val label: String,
    val category: String,
    val customCategory: String? = null,
    val installDate: Long,
    val lastUsed: Long,
    val sizeBytes: Long,
    val iconUri: String? = null,
    val usageScore: Float = 0f,
    val tags: List<TagModel> = emptyList()
) {
    val displayCategory: String
        get() = customCategory ?: category
}

data class TagModel(
    val id: String,
    val label: String,
    val colorHex: String
)

data class FolderRule(
    val id: String,
    val folderId: String,
    val type: RuleType,
    val value: String
)

data class FolderModel(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val colorToken: String, // Hex or palette name
    val isSystem: Boolean,
    val sortOrder: Int,
    val sortField: String, // "name", "installDate", "lastUsed", "size"
    val sortDirection: String, // "ASC", "DESC"
    val rules: List<FolderRule> = emptyList(),
    val apps: List<AppModel> = emptyList()
)
