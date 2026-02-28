package com.project.analyzer.chooser.presentation

import androidx.lifecycle.viewModelScope
import com.project.analyzer.chooser.domain.model.File
import com.project.analyzer.chooser.domain.model.TreeResult
import com.project.analyzer.chooser.domain.useCase.FileChooserUseCase
import com.project.analyzer.chooser.presentation.FileChooserIntent.ClickDrive
import com.project.analyzer.chooser.presentation.FileChooserIntent.ClickPlace
import com.project.analyzer.chooser.presentation.FileChooserIntent.Init
import com.project.analyzer.chooser.presentation.FileChooserIntent.OpenDirectory
import com.project.analyzer.chooser.presentation.FileChooserIntent.Refresh
import com.project.analyzer.chooser.presentation.FileChooserIntent.SelectEntry
import com.project.analyzer.chooser.presentation.FileChooserIntent.SelectPath
import com.project.analyzer.chooser.presentation.FileChooserIntent.ToggleExpand
import com.project.analyzer.chooser.presentation.FileChooserIntent.ToggleHidden
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import java.nio.file.Path

@Inject
internal class FileChooserViewModel(
    private val useCase: FileChooserUseCase,
) : LeakAwareMviViewModel<FileChooserIntent, FileUiState>(FileUiState()) {

    init {
        loadSidebar()
    }

    override suspend fun handleIntent(intent: FileChooserIntent) {
        when (intent) {
            is Init -> handleInit(intent)
            is ClickDrive -> handleOpenDrive(intent.drive)
            is OpenDirectory -> handleOpenDrive(intent.drive)
            is ClickPlace -> handleOpenPath(intent.place.path)
            is ToggleExpand -> handleToggleExpand(intent.dir)
            is SelectEntry -> handleSelectEntry(intent)
            is SelectPath -> updateState { copy(selected = intent.path) }
            is ToggleHidden -> handleToggleHidden()
            is Refresh -> handleRefresh()
        }
    }

    private fun loadSidebar() = launch {
        val data = useCase.loadSidebar()
        updateState { copy(drives = data.drives, places = data.places, error = null) }
    }

    private suspend fun handleInit(intent: Init) {
        updateState {
            copy(
                selectionMode = intent.selectionMode,
                error = null,
            )
        }
        intent.startPath?.let { handleOpenPath(it) }
    }

    private suspend fun handleOpenDrive(drive: File) {
        val result = useCase.openDrive(drive.path, drive.label, currentShowHidden)
        applyTreeResult(result, currentDir = drive.path, selectedDrive = drive.path)
    }

    private suspend fun handleToggleExpand(dirPath: String) {
        val result = useCase.toggleExpand(dirPath, currentShowHidden)
        val drive = resolveDrive(dirPath)
        applyTreeResult(result, currentDir = dirPath, selectedDrive = drive)
    }

    private fun handleSelectEntry(intent: SelectEntry) {
        if (intent.entry.isDirectory) {
            dispatch(ToggleExpand(intent.entry.path))
        } else {
            updateState { copy(selected = intent.entry.path) }
        }
    }

    private suspend fun handleOpenPath(targetPath: String) {
        val state = currentState
        val drive = findDriveFor(targetPath, state.drives) ?: return

        val result = useCase.openPath(
            targetPath = targetPath,
            drivePath = drive.path,
            driveLabel = drive.label,
            showHidden = currentShowHidden,
        )
        applyTreeResult(result, currentDir = targetPath, selectedDrive = drive.path)
    }

    private suspend fun handleToggleHidden() {
        updateState { copy(showHidden = !showHidden, error = null) }
        handleRefresh()
    }

    private suspend fun handleRefresh() {
        val result = useCase.refreshTree(currentShowHidden)
        updateState {
            copy(
                treeNodes = result.nodes,
                error = null,
            )
        }
    }

    private fun applyTreeResult(result: TreeResult, currentDir: String, selectedDrive: String) {
        updateState {
            copy(
                treeNodes = result.nodes,
                scrollToIndex = result.scrollToIndex,
                currentDir = currentDir,
                selectedDrive = selectedDrive,
                selected = "",
                error = null,
            )
        }
    }

    private fun resolveDrive(path: String): String {
        val drives = currentState.drives
        return drives
            .map { it.path }
            .filter { path.startsWith(it) }
            .maxByOrNull { it.length }
            ?: currentState.selectedDrive
    }

    private val currentShowHidden: Boolean get() = currentState.showHidden

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                updateState { copy(error = e.message) }
            }
        }
    }

    private fun findDriveFor(path: String, drives: List<File>): File? {
        return drives
            .filter { path.startsWith(it.path) }
            .maxByOrNull { it.path.length }
            ?: run {
                val root = Path.of(path).root?.toString() ?: return@run null
                File(label = root, path = root)
            }
    }
}
