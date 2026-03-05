package com.project.analyzer.utils.file

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileExtTest {

    @Test
    fun `copyDirectoryWithRollback copies directory and removes source`() {
        val root = Files.createTempDirectory("file-ext-test").toFile()
        try {
            val source = File(root, "source").apply {
                mkdirs()
                File(this, "data.txt").writeText("payload")
                File(this, "nested").mkdirs()
                File(this, "nested/more.txt").writeText("nested")
            }
            val target = File(root, "target")

            val copied = source.copyDirectoryWithRollback(target)

            assertTrue(copied)
            assertFalse(source.exists())
            assertEquals("payload", File(target, "data.txt").readText())
            assertEquals("nested", File(target, "nested/more.txt").readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `copyDirectoryWithRollback returns true when source directory missing`() {
        val root = Files.createTempDirectory("file-ext-test-missing").toFile()
        try {
            val source = File(root, "missing-source")
            val target = File(root, "target").apply { mkdirs() }

            val copied = source.copyDirectoryWithRollback(target)

            assertTrue(copied)
            assertTrue(target.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
