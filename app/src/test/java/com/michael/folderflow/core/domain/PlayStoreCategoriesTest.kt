package com.michael.folderflow.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayStoreCategoriesTest {
    @Test
    fun `contains official Play app categories and game categories`() {
        val categoryNames = PlayStoreCategories.all.map { it.name }

        assertTrue("Art and Design" in categoryNames)
        assertTrue("Communications" in categoryNames)
        assertTrue("Health and Fitness" in categoryNames)
        assertTrue("Video Players and Editors" in categoryNames)
        assertTrue("Action" in categoryNames)
        assertTrue("Role Playing" in categoryNames)
        assertTrue("Word" in categoryNames)
    }

    @Test
    fun `classifies apps into Play category names from package and label`() {
        assertEquals(
            "Communications",
            PlayStoreCategories.classify("com.whatsapp", "WhatsApp", null)
        )
        assertEquals(
            "Music and Audio",
            PlayStoreCategories.classify("com.spotify.music", "Spotify", null)
        )
        assertEquals(
            "Puzzle",
            PlayStoreCategories.classify("com.king.candycrushsaga", "Candy Crush Saga", "Games")
        )
        assertEquals(
            "Productivity",
            PlayStoreCategories.classify("com.unknown.notes", "Quiet Notes", "Productivity")
        )
    }
}
