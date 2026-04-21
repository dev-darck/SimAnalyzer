package com.analyzer.settings.data.lmu

import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipInputStream

@Inject
@SingleIn(ScreenScope::class)
internal class LmuPluginArchiveValidator(@param:IO private val ioDispatcher: CoroutineDispatcher) {

    suspend fun extractPluginDll(archive: LmuPluginArchive): LmuPluginDll = withContext(ioDispatcher) {
        ZipInputStream(Files.newInputStream(archive.path).buffered()).use { zipInput ->
            while (true) {
                val entry = zipInput.nextEntry ?: break
                val entryName = entry.name.substringAfterLast('/')
                if (!entry.isDirectory && entryName == LMU_PLUGIN_DLL_NAME) {
                    val bytes = ByteArrayOutputStream().use { output ->
                        zipInput.copyTo(output)
                        output.toByteArray()
                    }
                    validatePluginDll(bytes)
                    return@withContext LmuPluginDll(bytes = bytes, sha256 = bytes.sha256Hex())
                }
                zipInput.closeEntry()
            }
        }
        error("Official plugin archive does not contain $LMU_PLUGIN_DLL_NAME.")
    }

    private fun validatePluginDll(bytes: ByteArray) {
        check(bytes.size >= MIN_DLL_SIZE_BYTES) { "Downloaded plugin DLL is unexpectedly small." }
        check(bytes[0] == 'M'.code.toByte() && bytes[1] == 'Z'.code.toByte()) {
            "Downloaded plugin is not a Windows PE DLL."
        }
        val peOffset = bytes.readIntLe(PE_POINTER_OFFSET)
        check(peOffset > 0 && peOffset + PE_HEADER_MIN_SIZE < bytes.size) {
            "Downloaded plugin has an invalid PE header."
        }
        check(
            bytes[peOffset] == 'P'.code.toByte() &&
                bytes[peOffset + 1] == 'E'.code.toByte() &&
                bytes[peOffset + 2] == 0.toByte() &&
                bytes[peOffset + 3] == 0.toByte(),
        ) {
            "Downloaded plugin has an invalid PE signature."
        }
        check(bytes.readUShortLe(peOffset + PE_MACHINE_OFFSET) == PE_MACHINE_AMD64) {
            "Downloaded plugin is not a 64-bit Windows DLL."
        }
        check(bytes.readUShortLe(peOffset + PE_OPTIONAL_HEADER_OFFSET) == PE_OPTIONAL_HEADER_PE32_PLUS) {
            "Downloaded plugin is not a PE32+ x64 image."
        }
        val characteristics = bytes.readUShortLe(peOffset + PE_CHARACTERISTICS_OFFSET)
        check((characteristics and PE_CHARACTERISTIC_DLL) != 0) {
            "Downloaded plugin image is not marked as a DLL."
        }
    }

    private fun ByteArray.readIntLe(offset: Int): Int = (this[offset].toInt() and 0xff) or
        ((this[offset + 1].toInt() and 0xff) shl 8) or
        ((this[offset + 2].toInt() and 0xff) shl 16) or
        ((this[offset + 3].toInt() and 0xff) shl 24)

    private fun ByteArray.readUShortLe(offset: Int): Int =
        (this[offset].toInt() and 0xff) or ((this[offset + 1].toInt() and 0xff) shl 8)

    private companion object {
        const val MIN_DLL_SIZE_BYTES = 16 * 1024
        const val PE_POINTER_OFFSET = 0x3c
        const val PE_HEADER_MIN_SIZE = 26
        const val PE_MACHINE_OFFSET = 4
        const val PE_CHARACTERISTICS_OFFSET = 22
        const val PE_OPTIONAL_HEADER_OFFSET = 24
        const val PE_MACHINE_AMD64 = 0x8664
        const val PE_OPTIONAL_HEADER_PE32_PLUS = 0x20b
        const val PE_CHARACTERISTIC_DLL = 0x2000
    }
}
