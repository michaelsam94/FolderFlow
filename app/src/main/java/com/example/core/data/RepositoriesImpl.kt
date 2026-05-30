package com.example.core.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.core.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AppRepositoryImpl(
    private val context: Context,
    private val appDao: AppDao,
    private val tagDao: TagDao
) : AppRepository {

    override fun getAllApps(): Flow<List<AppModel>> {
        return combine(
            appDao.getAllAppsFlow(),
            tagDao.getAllTagsFlow(),
            tagDao.getAllAppTagCrossRefsFlow()
        ) { apps, tags, refs ->
            val tagMap = tags.associateBy { it.tagId }
            val refMap = refs.groupBy { it.packageName }
            
            apps.map { entity ->
                val appTags = refMap[entity.packageName]?.mapNotNull { ref ->
                    tagMap[ref.tagId]?.let { TagModel(it.tagId, it.label, it.colorHex) }
                } ?: emptyList()

                AppModel(
                    packageName = entity.packageName,
                    label = entity.label,
                    category = entity.category,
                    customCategory = entity.customCategory,
                    installDate = entity.installDate,
                    lastUsed = entity.lastUsed,
                    sizeBytes = entity.sizeBytes,
                    iconUri = entity.iconUri,
                    usageScore = entity.usageScore,
                    tags = appTags
                )
            }
        }
    }

    override fun getApp(packageName: String): Flow<AppModel?> {
        return getAllApps().map { list -> list.find { it.packageName == packageName } }
    }

    override suspend fun scanAndSyncApps(mockIfLowCount: Boolean) = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        
        val entities = mutableListOf<AppEntity>()
        
        for (info in resolveInfos) {
            val packageName = info.activityInfo.packageName
            val label = info.loadLabel(pm).toString()
            
            // Attempt to get size and install date
            var installDate = System.currentTimeMillis()
            var sizeBytes = 10_000_000L // default 10MB
            try {
                val packageInfo = pm.getPackageInfo(packageName, 0)
                installDate = packageInfo.firstInstallTime
                packageInfo.applicationInfo?.sourceDir?.let { sDir ->
                    val file = File(sDir)
                    if (file.exists()) {
                        sizeBytes = file.length()
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }

            // Get category
            val category = classifyPackage(packageName, label)
            
            // Find existing to preserve custom category and last used
            val existing = appDao.getAppByPackage(packageName)
            val lastUsed = existing?.lastUsed ?: installDate
            val usageScore = existing?.usageScore ?: 0f
            val customCategory = existing?.customCategory

            entities.add(
                AppEntity(
                    packageName = packageName,
                    label = label,
                    category = category,
                    customCategory = customCategory,
                    installDate = installDate,
                    lastUsed = lastUsed,
                    sizeBytes = sizeBytes,
                    iconUri = null, // Will use resolve info dynamically
                    usageScore = usageScore
                )
            )
        }

        if (entities.isNotEmpty()) {
            appDao.insertApps(entities)
        }

        // If very low apps, seed mocks to make the dashboard full of beautiful telemetry in emulator
        if (mockIfLowCount && (entities.size < 15)) {
            seedMockApps()
        }
    }

    override suspend fun overrideCategory(packageName: String, customCategory: String?) {
        withContext(Dispatchers.IO) {
            appDao.getAppByPackage(packageName)?.let { existing ->
                appDao.insertApp(existing.copy(customCategory = customCategory))
            }
        }
    }

    override suspend fun seedMockApps() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val oneHour = 60 * 60 * 1000L
        val oneDay = 24 * oneHour
        
        val mockApps = listOf(
            AppEntity("com.whatsapp", "WhatsApp", "Social", null, now - 180 * oneDay, now - 2 * oneHour, 45_000_000L, null, 98f),
            AppEntity("com.instagram", "Instagram", "Social", null, now - 150 * oneDay, now - 15 * oneHour, 95_000_000L, null, 90f),
            AppEntity("com.twitter", "X / Twitter", "Social", null, now - 120 * oneDay, now - 1 * oneDay, 60_000_000L, null, 70f),
            AppEntity("com.discord", "Discord", "Social", null, now - 90 * oneDay, now - 3 * oneHour, 110_000_000L, null, 85f),
            AppEntity("com.slack", "Slack", "Social", null, now - 80 * oneDay, now - 4 * oneHour, 85_000_000L, null, 80f),
            
            AppEntity("com.chase", "Chase Mobile", "Finance", null, now - 200 * oneDay, now - 2 * oneDay, 125_000_000L, null, 60f),
            AppEntity("com.paypal", "PayPal", "Finance", null, now - 210 * oneDay, now - 25 * oneDay, 55_000_000L, null, 40f),
            AppEntity("com.robinhood", "Robinhood", "Finance", null, now - 100 * oneDay, now - 35 * oneDay, 75_000_000L, null, 15f), // Unused > 30 days
            AppEntity("com.cashapp", "Cash App", "Finance", null, now - 60 * oneDay, now - 5 * oneDay, 40_000_000L, null, 50f),
            
            AppEntity("com.subway.surfers", "Subway Surfers", "Games", null, now - 140 * oneDay, now - 95 * oneDay, 185_000_000L, null, 2f), // Unused > 90 days
            AppEntity("com.candycrush", "Candy Crush", "Games", null, now - 110 * oneDay, now - 3 * oneDay, 210_000_000L, null, 65f),
            AppEntity("com.minecraft", "Minecraft", "Games", null, now - 50 * oneDay, now - 12 * oneHour, 280_000_000L, null, 92f),
            AppEntity("com.chess", "Chess.com", "Games", null, now - 40 * oneDay, now - 72 * oneDay, 50_000_000L, null, 5f), // Unused > 60 days
            
            AppEntity("com.spotify", "Spotify", "Media", null, now - 190 * oneDay, now - 15 * 60000L, 82_000_000L, null, 95f),
            AppEntity("com.netflix", "Netflix", "Media", null, now - 160 * oneDay, now - 3 * oneDay, 130_000_000L, null, 55f),
            AppEntity("com.youtube", "YouTube", "Media", null, now - 300 * oneDay, now - 10 * 60000L, 160_000_000L, null, 99f),
            
            AppEntity("com.amazon", "Amazon Shopping", "Shopping", null, now - 240 * oneDay, now - 12 * oneDay, 115_000_000L, null, 45f),
            AppEntity("com.ebay", "eBay", "Shopping", null, now - 100 * oneDay, now - 45 * oneDay, 70_000_000L, null, 8f), // Unused > 30 days
            
            AppEntity("com.fitbit", "Fitbit", "Health", null, now - 120 * oneDay, now - 1 * oneDay, 90_000_000L, null, 75f),
            AppEntity("com.calm", "Calm", "Health", null, now - 15 * oneDay, now - 42 * oneDay, 64_000_000L, null, 12f), // Unused > 30 days
            AppEntity("com.strava", "Strava Tracker", "Health", null, now - 6 * oneDay, now - 10 * oneHour, 98_000_000L, null, 88f), // Installed last 7 days
            
            AppEntity("com.chrome", "Chrome Browser", "Tools", null, now - 350 * oneDay, now - 5 * 60000L, 195_000_000L, null, 97f),
            AppEntity("com.drive", "Google Drive", "Tools", null, now - 320 * oneDay, now - 2 * oneDay, 88_000_000L, null, 50f),
            AppEntity("com.cleaner.pro", "Super Cleaner Pro", "Tools", null, now - 1 * oneDay, now - 12 * oneHour, 15_000_000L, null, 90f), // Installed last 7 days
            
            AppEntity("com.util.calculator", "Smart Calculator", "Utilities", null, now - 3 * oneDay, now - 2 * oneHour, 8_000_000L, null, 85f), // Installed last 7 days
            AppEntity("com.util.calendar", "Simple Calendar", "Utilities", null, now - 400 * oneDay, now - 4 * oneHour, 18_000_000L, null, 78f),
            AppEntity("com.weather.direct", "Direct Weather", "Utilities", null, now - 4 * oneDay, now - 3 * oneHour, 22_000_000L, null, 82f), // Installed last 7 days
            AppEntity("com.focus.timer", "Zen Focus Clock", "Utilities", null, now - 10 * oneDay, now - 50 * oneDay, 12_000_000L, null, 4f) // Unused > 30 days
        )
        
        appDao.insertApps(mockApps)
    }

    private fun classifyPackage(packageName: String, label: String): String {
        val pm = context.packageManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                when (appInfo.category) {
                    ApplicationInfo.CATEGORY_GAME -> return "Games"
                    ApplicationInfo.CATEGORY_AUDIO,
                    ApplicationInfo.CATEGORY_VIDEO,
                    ApplicationInfo.CATEGORY_IMAGE -> return "Media"
                    ApplicationInfo.CATEGORY_SOCIAL -> return "Social"
                    ApplicationInfo.CATEGORY_PRODUCTIVITY -> return "Tools"
                    ApplicationInfo.CATEGORY_NEWS,
                    ApplicationInfo.CATEGORY_MAPS -> return "Utilities"
                }
            }
        } catch (e: Exception) {
            // Fallback
        }

        // Precise Keyword Local Rule-Based Classifier
        val combined = "$packageName $label".lowercase()
        return when {
            combined.containsAny("chat", "social", "contact", "messenger", "facebook", "twitter", "instagram", "mastodon", "whatsapp", "signal", "telegram", "discord", "linkedin", "tiktok", "snapchat", "reddit", "meet", "zoom", "skype", "teams", "slack") -> "Social"
            combined.containsAny("bank", "finance", "pay", "wallet", "crypto", "stock", "invest", "money", "card", "cash", "ledger", "paypal", "venmo", "chase", "crypto") -> "Finance"
            combined.containsAny("game", "play", "arcade", "puzzle", "action", "rpg", "adventure", "chess", "sudoku", "solitaire", "mine", "craft", "steam", "nintendo") -> "Games"
            combined.containsAny("shop", "store", "buy", "cart", "amazon", "ebay", "market", "shopify", "target", "walmart", "aliexpress") -> "Shopping"
            combined.containsAny("health", "fit", "diet", "doctor", "workout", "run", "sleep", "heart", "gym", "meditation", "serene", "yoga", "calm") -> "Health"
            combined.containsAny("music", "video", "player", "stream", "tv", "movie", "radio", "podcast", "camera", "photo", "editor", "gallery", "spotify", "youtube", "netflix", "hulu", "vlc") -> "Media"
            combined.containsAny("utility", "calc", "clock", "weather", "compass", "note", "task", "calendar", "alarm", "timer", "file", "terminal", "compiler", "debug", "test", "benchmark", "cleaner", "optimizer", "backup", "setting", "info", "assistant") -> "Utilities"
            else -> "Tools"
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}

class FolderRepositoryImpl(
    private val folderDao: FolderDao,
    private val appRepository: AppRepository
) : FolderRepository {

    override fun getAllFolders(): Flow<List<FolderModel>> {
        return combine(
            folderDao.getAllFoldersFlow(),
            folderDao.getAllRulesFlow(),
            appRepository.getAllApps()
        ) { folders, rules, apps ->
            val ruleMap = rules.groupBy { it.folderId }
            folders.map { entity ->
                val designRules = ruleMap[entity.folderId]?.map { r ->
                    FolderRule(r.ruleId, r.folderId, RuleType.valueOf(r.ruleType), r.ruleValue)
                } ?: emptyList()

                // Filter the apps based on rules
                val matchingApps = if (designRules.isEmpty()) {
                    // Static folder, but since all FolderFlow is rule-based:
                    // Treat isSystem & no rule folders as matching category or empty
                    emptyList()
                } else {
                    apps.filter { app ->
                        designRules.all { rule ->
                            when (rule.type) {
                                RuleType.CATEGORY -> app.displayCategory.equals(rule.value, ignoreCase = true)
                                RuleType.TAG -> app.tags.any { t -> t.label.equals(rule.value, ignoreCase = true) }
                                RuleType.INSTALL_RECENT -> {
                                    val days = rule.value.toLongOrNull() ?: 7
                                    val threshold = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
                                    app.installDate >= threshold
                                }
                                RuleType.UNUSED_THRESHOLD -> {
                                    val days = rule.value.toLongOrNull() ?: 30
                                    val threshold = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
                                    app.lastUsed < threshold
                                }
                            }
                        }
                    }
                }

                // Sort matching apps
                val sortedApps = when (entity.sortField) {
                    "name" -> {
                        if (entity.sortDirection == "ASC") matchingApps.sortedBy { it.label.lowercase() }
                        else matchingApps.sortedByDescending { it.label.lowercase() }
                    }
                    "installDate" -> {
                        if (entity.sortDirection == "ASC") matchingApps.sortedBy { it.installDate }
                        else matchingApps.sortedByDescending { it.installDate }
                    }
                    "lastUsed" -> {
                        if (entity.sortDirection == "ASC") matchingApps.sortedBy { it.lastUsed }
                        else matchingApps.sortedByDescending { it.lastUsed }
                    }
                    "size" -> {
                        if (entity.sortDirection == "ASC") matchingApps.sortedBy { it.sizeBytes }
                        else matchingApps.sortedByDescending { it.sizeBytes }
                    }
                    else -> matchingApps
                }

                FolderModel(
                    id = entity.folderId,
                    name = entity.name,
                    iconEmoji = entity.iconEmoji,
                    colorToken = entity.colorToken,
                    isSystem = entity.isSystem,
                    sortOrder = entity.sortOrder,
                    sortField = entity.sortField,
                    sortDirection = entity.sortDirection,
                    rules = designRules,
                    apps = sortedApps
                )
            }
        }
    }

    override fun getFolder(folderId: String): Flow<FolderModel?> {
        return getAllFolders().map { list -> list.find { it.id == folderId } }
    }

    override suspend fun createFolder(
        name: String,
        iconEmoji: String,
        colorToken: String,
        rules: List<FolderRule>
    ): String = withContext(Dispatchers.IO) {
        val folderId = UUID.randomUUID().toString()
        val totalFolders = folderDao.getAllFolders().size
        
        folderDao.insertFolder(
            FolderEntity(
                folderId = folderId,
                name = name,
                iconEmoji = iconEmoji,
                colorToken = colorToken,
                isSystem = false,
                sortOrder = totalFolders,
                sortField = "name",
                sortDirection = "ASC"
            )
        )

        val ruleEntities = rules.map { rule ->
            FolderRuleEntity(
                ruleId = UUID.randomUUID().toString(),
                folderId = folderId,
                ruleType = rule.type.name,
                ruleValue = rule.value
            )
        }
        folderDao.insertRules(ruleEntities)
        folderId
    }

    override suspend fun deleteFolder(folderId: String) = withContext(Dispatchers.IO) {
        folderDao.deleteRulesForFolder(folderId)
        folderDao.deleteFolder(folderId)
    }

    override suspend fun updateFolderSort(
        folderId: String,
        sortField: String,
        sortDirection: String
    ) {
        withContext(Dispatchers.IO) {
            folderDao.getFolderById(folderId)?.let { existing ->
                folderDao.insertFolder(
                    existing.copy(sortField = sortField, sortDirection = sortDirection)
                )
            }
        }
    }

    override suspend fun seedDefaultSystemFolders() = withContext(Dispatchers.IO) {
        val existing = folderDao.getAllFolders()
        if (existing.isNotEmpty()) return@withContext

        val defaults = listOf(
            Triple("Social", "💬", "#2196F3"),
            Triple("Finance", "💵", "#4CAF50"),
            Triple("Games", "🎮", "#FF9800"),
            Triple("Tools", "⚙️", "#9C27B0"),
            Triple("Shopping", "🛍️", "#E91E63"),
            Triple("Health", "❤️", "#F44336"),
            Triple("Media", "🎬", "#3F51B5"),
            Triple("Utilities", "🛠️", "#009688")
        )

        defaults.forEachIndexed { index, (name, icon, color) ->
            val folderId = "system_$name".lowercase()
            folderDao.insertFolder(
                FolderEntity(
                    folderId = folderId,
                    name = name,
                    iconEmoji = icon,
                    colorToken = color,
                    isSystem = true,
                    sortOrder = index,
                    sortField = "name",
                    sortDirection = "ASC"
                )
            )
            // Add Category Rule
            folderDao.insertRule(
                FolderRuleEntity(
                    ruleId = UUID.randomUUID().toString(),
                    folderId = folderId,
                    ruleType = "CATEGORY",
                    ruleValue = name
                )
            )
        }

        // Add special System folders
        // 1. New Apps
        val newFolderId = "system_new"
        folderDao.insertFolder(
            FolderEntity(
                folderId = newFolderId,
                name = "New Apps",
                iconEmoji = "📦",
                colorToken = "#FFEB3B",
                isSystem = true,
                sortOrder = defaults.size,
                sortField = "installDate",
                sortDirection = "DESC"
            )
        )
        folderDao.insertRule(
            FolderRuleEntity(
                ruleId = UUID.randomUUID().toString(),
                folderId = newFolderId,
                ruleType = "INSTALL_RECENT",
                ruleValue = "7"
            )
        )

        // 2. Unused Apps
        val unusedFolderId = "system_unused"
        folderDao.insertFolder(
            FolderEntity(
                folderId = unusedFolderId,
                name = "Unused Apps",
                iconEmoji = "💤",
                colorToken = "#795548",
                isSystem = true,
                sortOrder = defaults.size + 1,
                sortField = "lastUsed",
                sortDirection = "ASC"
            )
        )
        folderDao.insertRule(
            FolderRuleEntity(
                ruleId = UUID.randomUUID().toString(),
                folderId = unusedFolderId,
                ruleType = "UNUSED_THRESHOLD",
                ruleValue = "30"
            )
        )
    }
}

class TagRepositoryImpl(
    private val tagDao: TagDao
) : TagRepository {

    override fun getAllTags(): Flow<List<TagModel>> {
        return tagDao.getAllTagsFlow().map { entities ->
            entities.map { TagModel(it.tagId, it.label, it.colorHex) }
        }
    }

    override suspend fun createTag(label: String, colorHex: String): String = withContext(Dispatchers.IO) {
        val tagId = UUID.randomUUID().toString()
        tagDao.insertTag(TagEntity(tagId, label, colorHex))
        tagId
    }

    override suspend fun deleteTag(tagId: String) = withContext(Dispatchers.IO) {
        tagDao.deleteTag(tagId)
    }

    override suspend fun addTagToApp(packageName: String, tagId: String) = withContext(Dispatchers.IO) {
        tagDao.insertCrossRef(AppTagCrossRef(packageName, tagId))
    }

    override suspend fun removeTagFromApp(packageName: String, tagId: String) = withContext(Dispatchers.IO) {
        tagDao.deleteCrossRef(packageName, tagId)
    }
}
