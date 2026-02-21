@file:OptIn(ExperimentalPathApi::class)

package com.project.analyzer.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.system.exitProcess

public object SingleInstanceGuard {

    private var channel: FileChannel? = null
    private var lock: FileLock? = null

    private val lockPath = Paths.get(
        System.getProperty("user.home"),
        ".simanalyzer${if (BuildConfig.IS_DEBUG) "-debug" else ""}",
        "app.lock",
    )

    public suspend fun acquireOrExit(): Unit = withContext(Dispatchers.IO) {
        Files.createDirectories(lockPath.parent)

        channel = FileChannel.open(
            lockPath,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
        )

        lock = try {
            channel!!.tryLock()
        } catch (_: OverlappingFileLockException) {
            null
        }

        if (lock == null) {
            channel?.close()
            exitProcess(0)
        }
    }

    public suspend fun release(): Unit = withContext(Dispatchers.IO) {
        try {
            lock?.release()
        } catch (_: Throwable) {
        }
        try {
            channel?.close()
        } catch (_: Throwable) {
        }
        lockPath.parent.deleteRecursively()
    }
}
