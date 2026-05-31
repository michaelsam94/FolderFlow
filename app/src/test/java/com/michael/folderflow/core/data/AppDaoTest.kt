package com.michael.folderflow.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppDaoTest {
    @Test
    fun `upserting existing app preserves tag cross references`() = runTest {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()

        try {
            val appDao = database.appDao()
            val tagDao = database.tagDao()
            val originalApp = AppEntity(
                packageName = "com.notes",
                label = "Notes",
                category = "Tools",
                installDate = 1L,
                lastUsed = 1L,
                sizeBytes = 1_024L
            )

            appDao.insertApp(originalApp)
            tagDao.insertTag(TagEntity("tag-work", "Work", "#3B82F6"))
            tagDao.insertCrossRef(AppTagCrossRef("com.notes", "tag-work"))

            appDao.insertApp(originalApp.copy(label = "Notes Updated"))

            assertEquals(
                listOf(AppTagCrossRef("com.notes", "tag-work")),
                tagDao.getAllAppTagCrossRefs()
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `upserting existing folder preserves folder rules`() = runTest {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()

        try {
            val folderDao = database.folderDao()
            val originalFolder = FolderEntity(
                folderId = "folder-work",
                name = "Work",
                iconEmoji = "W",
                colorToken = "#3B82F6",
                isSystem = false,
                sortOrder = 0,
                sortField = "name",
                sortDirection = "ASC"
            )

            folderDao.insertFolder(originalFolder)
            folderDao.insertRule(
                FolderRuleEntity(
                    ruleId = "rule-work",
                    folderId = "folder-work",
                    ruleType = "TAG",
                    ruleValue = "Work"
                )
            )

            folderDao.insertFolder(originalFolder.copy(sortField = "size", sortDirection = "DESC"))

            assertEquals(
                listOf(
                    FolderRuleEntity(
                        ruleId = "rule-work",
                        folderId = "folder-work",
                        ruleType = "TAG",
                        ruleValue = "Work"
                    )
                ),
                folderDao.getRulesForFolder("folder-work")
            )
        } finally {
            database.close()
        }
    }
}
