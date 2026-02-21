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
import com.project.analyzer.leak.api.LeakAwareViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.nio.file.Path

@Inject
class FileChooserViewModel(private val useCase: FileChooserUseCase) : LeakAwareViewModel() {

    private val _uiState = MutableStateFlow(FileUiState())
    val state: StateFlow<FileUiState> = _uiState.asStateFlow()

    init {
        loadSidebar()
    }

    fun dispatch(intent: FileChooserIntent) {
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
        updateState { copy(drives = data.drives, places = data.places) }
    }

    private fun handleInit(intent: Init) {
        updateState {
            copy(
                selectionMode = intent.selectionMode,
            )
        }
        intent.startPath?.let { handleOpenPath(it) }
    }

    private fun handleOpenDrive(drive: File) = launch {
        val result = useCase.openDrive(drive.path, drive.label, currentShowHidden)
        applyTreeResult(result, currentDir = drive.path, selectedDrive = drive.path)
    }

    private fun handleToggleExpand(dirPath: String) = launch {
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

    private fun handleOpenPath(targetPath: String) = launch {
        val state = _uiState.value
        val drive = findDriveFor(targetPath, state.drives) ?: return@launch

        val result = useCase.openPath(
            targetPath = targetPath,
            drivePath = drive.path,
            driveLabel = drive.label,
            showHidden = currentShowHidden,
        )
        applyTreeResult(result, currentDir = targetPath, selectedDrive = drive.path)
    }

    private fun handleToggleHidden() {
        updateState { copy(showHidden = !showHidden) }
        handleRefresh()
    }

    private fun handleRefresh() = launch {
        val result = useCase.refreshTree(currentShowHidden)
        updateState {
            copy(
                treeNodes = result.nodes,
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
            )
        }
    }

    private fun resolveDrive(path: String): String {
        val drives = _uiState.value.drives
        return drives
            .map { it.path }
            .filter { path.startsWith(it) }
            .maxByOrNull { it.length }
            ?: _uiState.value.selectedDrive
    }

    private val currentShowHidden: Boolean get() = _uiState.value.showHidden

    private inline fun updateState(crossinline block: FileUiState.() -> FileUiState) {
        _uiState.update { it.block() }
    }

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
