package com.project.analyzer.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.system.exitProcess

public object SingleInstanceGuard {

    private var channel: FileChannel? = null
    private var lock: FileLock? = null
    private var lockPath: Path? = null

    public suspend fun acquireOrExit(lockFile: File, dispatcher: CoroutineDispatcher): Unit = withContext(dispatcher) {
        val path = lockFile.toPath()
        Files.createDirectories(path.parent)

        channel = FileChannel.open(
            path,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
        )
        lockPath = path

        val ch = channel ?: return@withContext
        lock = try {
            ch.tryLock()
        } catch (_: OverlappingFileLockException) {
            null
        }

        if (lock == null) {
            channel?.close()
            channel = null
            lockPath = null
            exitProcess(0)
        }
    }

    public suspend fun release(dispatcher: CoroutineDispatcher): Unit = withContext(dispatcher) {
        val currentLockPath = lockPath
        try {
            lock?.release()
        } catch (_: Throwable) {
        }
        lock = null
        try {
            channel?.close()
        } catch (_: Throwable) {
        }
        channel = null
        lockPath = null
        if (currentLockPath == null) return@withContext
        runCatching { Files.deleteIfExists(currentLockPath) }
        try {
            Files.deleteIfExists(currentLockPath.parent)
        } catch (_: DirectoryNotEmptyException) {
        }
    }
}
