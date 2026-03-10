package com.project.analyzer.ac.telemetry.impl.trackmap.evo

import dev.zacsweers.metro.Inject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.Path

@Inject
internal class AcEvoKspkgReader {

    fun open(packagePath: Path): AcEvoKspkgArchive {
        val totalSizeBytes = Files.size(packagePath)
        require(totalSizeBytes > TABLE_SIZE_BYTES) {
            "Invalid AC EVO content package size: $totalSizeBytes"
        }

        val xorKey = readRange(
            packagePath = packagePath,
            offsetBytes = totalSizeBytes - XOR_KEY_SIZE_BYTES,
            lengthBytes = XOR_KEY_SIZE_BYTES.toInt(),
        )
        val tableBytes = readRange(
            packagePath = packagePath,
            offsetBytes = totalSizeBytes - TABLE_SIZE_BYTES,
            lengthBytes = TABLE_SIZE_BYTES.toInt(),
        )
        xorInPlace(tableBytes, xorKey)

        val entries = decodeEntries(tableBytes, totalSizeBytes - TABLE_SIZE_BYTES)
        return AcEvoKspkgArchive(
            packagePath = packagePath,
            xorKey = xorKey,
            entries = entries,
        )
    }

    private fun decodeEntries(tableBytes: ByteArray, tableOffsetBytes: Long): List<AcEvoKspkgEntry> {
        val buffer = ByteBuffer.wrap(tableBytes).order(ByteOrder.LITTLE_ENDIAN)
        return buildList(TABLE_ENTRY_COUNT) {
            repeat(TABLE_ENTRY_COUNT) { index ->
                val baseOffset = index * TABLE_ENTRY_SIZE_BYTES
                val pathLength = buffer.getShort(baseOffset + PATH_LENGTH_OFFSET).toInt() and 0xFFFF
                if (pathLength <= 0 || pathLength > PATH_MAX_LENGTH_BYTES) return@repeat

                val flags = buffer.getInt(baseOffset + FLAGS_OFFSET)
                val sizeBytes = buffer.getLong(baseOffset + SIZE_OFFSET)
                val dataOffsetBytes = buffer.getLong(baseOffset + DATA_OFFSET_OFFSET)
                if (sizeBytes <= 0L || dataOffsetBytes < 0L || dataOffsetBytes >= tableOffsetBytes) return@repeat

                val relativePath = tableBytes.copyOfRange(baseOffset, baseOffset + pathLength)
                    .toString(Charsets.UTF_8)
                    .replace('\\', '/')
                    .trim()
                if (relativePath.isBlank()) return@repeat

                add(
                    AcEvoKspkgEntry(
                        relativePath = relativePath,
                        flags = flags,
                        sizeBytes = sizeBytes,
                        dataOffsetBytes = dataOffsetBytes,
                    ),
                )
            }
        }
    }

    private fun readRange(packagePath: Path, offsetBytes: Long, lengthBytes: Int): ByteArray {
        require(lengthBytes >= 0) { "lengthBytes must be non-negative" }
        val bytes = ByteArray(lengthBytes)
        Files.newByteChannel(packagePath).use { channel ->
            channel.position(offsetBytes)
            readFully(channel, ByteBuffer.wrap(bytes))
        }
        return bytes
    }

    private fun xorInPlace(bytes: ByteArray, xorKey: ByteArray) {
        if (xorKey.isEmpty()) return
        for (index in bytes.indices) {
            bytes[index] = (bytes[index].toInt() xor xorKey[index % xorKey.size].toInt()).toByte()
        }
    }

    private fun readFully(channel: SeekableByteChannel, buffer: ByteBuffer) {
        while (buffer.hasRemaining()) {
            val read = channel.read(buffer)
            check(read >= 0) { "Unexpected EOF while reading ${channel.position()}" }
        }
    }

    private companion object {

        // AC EVO stores a fixed-size file table in the last 32 MB of the package.
        const val TABLE_SIZE_BYTES = 0x2000000L

        // The table is split into 131_072 slots, each exactly 256 bytes.
        const val TABLE_ENTRY_COUNT = 0x20000
        const val TABLE_ENTRY_SIZE_BYTES = 0x100

        // The XOR key is stored in the last 8 bytes of the package/table tail.
        const val XOR_KEY_SIZE_BYTES = 8L

        // Bytes [0x00..0xDF] of each entry are reserved for the UTF-8 relative path.
        const val PATH_MAX_LENGTH_BYTES = 0xE0

        // Entry layout offsets inside the 256-byte table row.
        const val FLAGS_OFFSET = 0xE4
        const val PATH_LENGTH_OFFSET = 0xE6
        const val SIZE_OFFSET = 0xF0
        const val DATA_OFFSET_OFFSET = 0xF8
    }
}

internal data class AcEvoKspkgEntry(
    val relativePath: String,
    val flags: Int,
    val sizeBytes: Long,
    val dataOffsetBytes: Long,
)

internal class AcEvoKspkgArchive internal constructor(
    private val packagePath: Path,
    private val xorKey: ByteArray,
    val entries: List<AcEvoKspkgEntry>,
) {

    fun extract(entry: AcEvoKspkgEntry): ByteArray {
        require(entry.sizeBytes <= Int.MAX_VALUE) {
            "Entry ${entry.relativePath} is too large to extract into memory: ${entry.sizeBytes}"
        }
        val bytes = ByteArray(entry.sizeBytes.toInt())
        Files.newByteChannel(packagePath).use { channel ->
            channel.position(entry.dataOffsetBytes)
            val buffer = ByteBuffer.wrap(bytes)
            while (buffer.hasRemaining()) {
                val read = channel.read(buffer)
                check(read >= 0) { "Unexpected EOF while extracting ${entry.relativePath}" }
            }
        }
        if ((entry.flags and XOR_DATA_FLAG) != 0) {
            for (index in bytes.indices) {
                bytes[index] = (bytes[index].toInt() xor xorKey[index % xorKey.size].toInt()).toByte()
            }
        }
        return bytes
    }

    private companion object {

        const val XOR_DATA_FLAG = 0x100
    }
}
