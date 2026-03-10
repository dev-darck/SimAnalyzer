package com.project.analyzer.utils.file

import java.io.File

public fun File.copyDirectoryWithRollback(targetDirectory: File): Boolean {
    if (!exists() || !isDirectory) return true
    if (isSameDirectory(targetDirectory)) return true

    val targetExistedBefore = targetDirectory.exists()
    if (!targetExistedBefore && !targetDirectory.mkdirs()) return false

    val preExistingTargetEntries = if (targetExistedBefore) {
        targetDirectory.relativeEntries()
    } else {
        emptySet()
    }
    val rollbackDirectory = buildRollbackDirectory()
    if (!renameTo(rollbackDirectory)) return false

    return runCatching {
        if (!rollbackDirectory.copyRecursively(target = targetDirectory, overwrite = false)) {
            error("Failed to copy telemetry directory")
        }
        if (!rollbackDirectory.deleteRecursively()) {
            error("Failed to delete old telemetry directory")
        }
        true
    }.getOrElse {
        rollbackTargetDirectory(
            targetDirectory = targetDirectory,
            targetExistedBefore = targetExistedBefore,
            preExistingTargetEntries = preExistingTargetEntries,
        )
        restoreSourceDirectory(rollbackDirectory = rollbackDirectory, sourceDirectory = this)
        false
    }
}

private fun File.buildRollbackDirectory(): File {
    val parent = parentFile ?: return File("$path.rollback")
    var attempt = 0
    while (true) {
        val suffix = if (attempt == 0) "" else "_$attempt"
        val candidate = File(parent, "$name.rollback$suffix")
        if (!candidate.exists()) return candidate
        attempt += 1
    }
}

private fun rollbackTargetDirectory(
    targetDirectory: File,
    targetExistedBefore: Boolean,
    preExistingTargetEntries: Set<String>,
) {
    if (!targetExistedBefore) {
        targetDirectory.deleteRecursively()
        return
    }
    if (!targetDirectory.exists()) return

    targetDirectory.walkBottomUp().forEach { entry ->
        if (entry == targetDirectory) return@forEach

        val relativePath = runCatching {
            entry.relativeTo(targetDirectory).invariantSeparatorsPath
        }.getOrNull() ?: return@forEach

        if (relativePath in preExistingTargetEntries) return@forEach
        runCatching { entry.deleteRecursively() }
    }
}

private fun restoreSourceDirectory(rollbackDirectory: File, sourceDirectory: File) {
    if (rollbackDirectory.renameTo(sourceDirectory)) return

    runCatching {
        rollbackDirectory.copyRecursively(target = sourceDirectory, overwrite = true)
        rollbackDirectory.deleteRecursively()
    }
}

private fun File.relativeEntries(): Set<String> = walkTopDown()
    .drop(1)
    .map { it.relativeTo(this).invariantSeparatorsPath }
    .toSet()

private fun File.isSameDirectory(other: File): Boolean = runCatching {
    canonicalFile == other.canonicalFile
}.getOrElse {
    absoluteFile == other.absoluteFile
}
