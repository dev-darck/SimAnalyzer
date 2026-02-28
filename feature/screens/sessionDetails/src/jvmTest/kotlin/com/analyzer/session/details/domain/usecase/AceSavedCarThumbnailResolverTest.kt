package com.analyzer.session.details.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class AceSavedCarThumbnailResolverTest {

    private var oldUserHome: String? = null

    @BeforeTest
    fun setUp() {
        oldUserHome = System.getProperty("user.home")
    }

    @AfterTest
    fun tearDown() {
        if (oldUserHome != null) {
            System.setProperty("user.home", oldUserHome!!)
        } else {
            System.clearProperty("user.home")
        }
    }

    @Test
    fun `resolve returns thumbnail from saved games by car model`() {
        val home = Files.createTempDirectory("ace-thumbnail-home").toFile()
        System.setProperty("user.home", home.absolutePath)

        val savedCarId = "4d51beff-b0bf-d582-c58c-0c1c13f86c81"
        File(
            home,
            "Saved Games/ACE/ProfileData/profile-1/OpenData/SavedCars/" +
                "ks_toyota_ae86_sprinter_trueno_${savedCarId}.carfinalstatewithconsumable",
        ).apply {
            parentFile?.mkdirs()
            writeText("test")
        }
        val texture = File(home, "Saved Games/ACE/SavedCars/Thumbnails/$savedCarId.texture").apply {
            parentFile?.mkdirs()
            writeText("test")
        }

        val match = AceSavedCarThumbnailResolver().resolve(
            gameId = "ACE",
            carModel = "ks_toyota_ae86_sprinter_trueno",
            sessionStartedAtMs = System.currentTimeMillis(),
        )

        assertEquals(savedCarId, match?.savedCarId)
        assertEquals(texture.absolutePath, match?.texturePath)
    }

    @Test
    fun `resolve returns null for non ace sessions`() {
        val match = AceSavedCarThumbnailResolver().resolve(
            gameId = "LMU",
            carModel = "ks_toyota_ae86_sprinter_trueno",
            sessionStartedAtMs = System.currentTimeMillis(),
        )

        assertNull(match)
    }
}
