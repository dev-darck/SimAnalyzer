package com.project.analyzer.chooser.data

import com.project.analyzer.api.di.IO
import com.project.analyzer.chooser.data.model.Candidate
import com.project.analyzer.chooser.data.model.SidebarInfo
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.filechooser.FileSystemView
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.io.path.name

@Inject
internal class FileSystemRepositoryImpl(
    @param:IO
    private val coroutineDispatcher: CoroutineDispatcher,
) : FileSystemRepository {

    override suspend fun loadSideBars(): SidebarInfo = withContext(coroutineDispatcher) {
        val fsv = FileSystemView.getFileSystemView()
        val candidates = buildCandidates(fsv)

        SidebarInfo(
            default = fsv.defaultDirectory.path,
            shortcutFiles = candidates.filter { !it.isDrive }.sortedBy { it.displayName },
            drives = candidates.filter { it.isDrive }.sortedBy { it.displayName },
        )
    }

    override suspend fun listSubdirectories(dir: Path, showHidden: Boolean): List<Path> =
        withContext(coroutineDispatcher) {
            runCatching {
                Files.list(dir).use { stream ->
                    stream.toList()
                        .filter { it.isDirectory() }
                        .filter { showHidden || !isHidden(it) }
                        .sortedBy { it.name.lowercase() }
                }
            }.getOrDefault(emptyList())
        }

    override suspend fun resolvePath(text: String): Path? = withContext(coroutineDispatcher) {
        runCatching {
            val path = Path.of(text.trim())
            if (Files.exists(path)) path.toAbsolutePath().normalize() else null
        }.getOrNull()
    }

    private fun isHidden(path: Path): Boolean = runCatching { path.isHidden() }.getOrDefault(false)

    private fun buildCandidates(fsv: FileSystemView): List<Candidate> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Candidate>()

        fun addIfNew(file: File) {
            val canonical = runCatching { file.canonicalPath }.getOrNull() ?: return

            if (!seen.add(canonical)) return

            val isDrive = runCatching { fsv.isDrive(file) }.getOrDefault(false)

            if (!isDrive && !(file.isDirectory && file.canRead())) return

            val displayName = runCatching { fsv.getSystemDisplayName(file) }
                .getOrDefault(file.name)
                .ifEmpty { file.name }

            val description = runCatching { fsv.getSystemTypeDescription(file) }
                .getOrDefault("")

            val isFileSystem = runCatching { fsv.isFileSystem(file) }.getOrDefault(true)

            result += Candidate(
                displayName = displayName,
                isDrive = isDrive,
                isFileSystem = isFileSystem,
                description = description,
                path = file.path,
            )
        }

        File.listRoots().forEach { addIfNew(it) }

        val listOfQA = fsv.chooserComboBoxFiles
        runCatching { listOfQA }
            .getOrDefault(emptyArray())
            .forEach { addIfNew(it) }

        val userHome = System.getProperty("user.home")?.let { File(it) }
        if (listOfQA.isNotEmpty() && userHome != null && userHome.exists()) {
            addIfNew(userHome)

            listOf("Desktop", "Documents", "Downloads", "Music", "Pictures", "Videos")
                .map { File(userHome, it) }
                .forEach { addIfNew(it) }
        }

        return result
    }
}
