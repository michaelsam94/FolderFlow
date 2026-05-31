package com.michael.folderflow.core.domain

import com.michael.folderflow.testutil.FakeAppRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UseCasesTest {
    @Test
    fun `unused apps are filtered by last used threshold`() = runTest {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        val active = app(packageName = "active", label = "Active", lastUsed = now - 3 * day)
        val stale = app(packageName = "stale", label = "Stale", lastUsed = now - 45 * day)
        val repo = FakeAppRepository(listOf(active, stale))

        val result = GetUnusedAppsUseCase(repo)(30).first()

        assertEquals(listOf("stale"), result.map { it.packageName })
    }

    @Test
    fun `display category prefers custom override`() {
        val model = app(category = "Tools", customCategory = "Finance")

        assertEquals("Finance", model.displayCategory)
    }

    private fun app(
        packageName: String = "pkg",
        label: String = "App",
        category: String = "Tools",
        customCategory: String? = null,
        lastUsed: Long = System.currentTimeMillis()
    ) = AppModel(
        packageName = packageName,
        label = label,
        category = category,
        customCategory = customCategory,
        installDate = lastUsed,
        lastUsed = lastUsed,
        sizeBytes = 1_024L
    )
}
