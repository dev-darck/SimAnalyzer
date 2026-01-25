package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.fallback.logfile.AcEvoLogLocator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertTrue

class AcEvoLogLocatorTest {

    private var oldUserHome: String? = null
    private var oldOverride: String? = null

    @BeforeTest
    fun setUp() {
        oldUserHome = System.getProperty("user.home")
        oldOverride = System.getProperty("acevo.logPath")
    }

    @AfterTest
    fun tearDown() {
        if (oldUserHome != null) System.setProperty("user.home", oldUserHome!!) else System.clearProperty("user.home")
        if (oldOverride != null) System.setProperty(
            "acevo.logPath",
            oldOverride!!
        ) else System.clearProperty("acevo.logPath")
    }

    @Test
    fun `override property acevo_logPath wins and is cached`() {
        val dir = Files.createTempDirectory("acevo-log-test").toFile()
        val f = File(dir, "log.txt").apply { writeText("hello\n") }

        System.setProperty("acevo.logPath", f.absolutePath)

        val locator = AcEvoLogLocator()
        val first = locator.locateLogFile()
        val second = locator.locateLogFile()

        assertEquals(f.absolutePath, first?.absolutePath)
        assertEquals(f.absolutePath, second?.absolutePath)
    }

    @Test
    fun `candidate under Documents ACE log txt is found`() {
        val home = Files.createTempDirectory("acevo-home").toFile()
        System.setProperty("user.home", home.absolutePath)

        val docs = File(home, "Documents")
        val aceDir = File(docs, "ACE")
        aceDir.mkdirs()

        val log = File(aceDir, "log.txt").apply { writeText("x\n") }

        val locator = AcEvoLogLocator()
        val found = locator.locateLogFile()

        assertEquals(log.absolutePath, found?.absolutePath)
    }

    @Test
    fun `cache is invalidated if override file disappears`() {
        val dir = Files.createTempDirectory("acevo-log-test2").toFile()
        val f = File(dir, "log.txt").apply { writeText("hello\n") }

        System.setProperty("acevo.logPath", f.absolutePath)

        val locator = AcEvoLogLocator()
        val first = locator.locateLogFile()
        assertEquals(f.absolutePath, first?.absolutePath)

        val oldPath = f.absolutePath
        f.delete()

        val second = locator.locateLogFile()

        assertTrue(
            second == null || second.absolutePath != oldPath,
            "returned cached path even though file was deleted"
        )
    }
}
