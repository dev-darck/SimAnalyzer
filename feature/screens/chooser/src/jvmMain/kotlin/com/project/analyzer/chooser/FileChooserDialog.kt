package com.project.analyzer.chooser

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.project.analyzer.chooser.presentation.FileChooserContent

@Composable
fun FileChooserDialog(
    state: FileChooserState,
) {
    if (!state.isVisible) return

    val dialogState = rememberDialogState(
        width = 920.dp,
        height = 720.dp
    )

    DialogWindow(
        onCloseRequest = { state.dismiss() },
        state = dialogState,
        title = state.title,
        resizable = true,
        undecorated = true,
        transparent = true,
        onKeyEvent = { keyEvent ->
            if (keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyUp) {
                state.dismiss()
                true
            } else {
                false
            }
        }
    ) {
        FileChooserContent(
            initialPath = state.initialPath,
            selectionMode = state.selectionMode,
            onConfirm = { path ->
                state.confirm(path)
            }
        )
    }
}
