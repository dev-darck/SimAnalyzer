package com.project.analyzer.chooser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

enum class SelectionMode {
    FILE,
    DIRECTORY
}

@Stable
class FileChooserState internal constructor(
    initialPath: String?,
    title: String,
    selectionMode: SelectionMode,
    private val onResult: (String?) -> Unit
) {

    var isVisible by mutableStateOf(false)
        private set

    var title by mutableStateOf(title)
        private set

    var initialPath by mutableStateOf(initialPath)
        internal set

    var selectionMode by mutableStateOf(selectionMode)
        private set

    fun show() {
        isVisible = true
    }

    fun dismiss() {
        isVisible = false
        onResult(null)
    }

    internal fun confirm(selectedPath: String) {
        isVisible = false
        onResult(selectedPath)
    }
}

@Composable
fun rememberFileChooserState(
    initialPath: String? = null,
    title: String = "Select file",
    selectionMode: SelectionMode = SelectionMode.FILE,
    onResult: (String?) -> Unit = {}
): FileChooserState {
    val linkToResult = rememberUpdatedState(onResult)

    return remember(initialPath, title) {
        FileChooserState(
            initialPath = initialPath,
            title = title,
            selectionMode = selectionMode,
            onResult = linkToResult.value
        )
    }
}
