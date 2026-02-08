package com.project.analyzer.chooser.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.chooser.SelectionMode
import com.project.analyzer.chooser.presentation.FileChooserIntent.SelectPath
import com.project.analyzer.chooser.presentation.FileChooserIntent.ToggleExpand
import com.project.analyzer.chooser.presentation.components.FileTree
import com.project.analyzer.chooser.presentation.components.Sidebar
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.textField.TextField
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
internal fun FileChooserContent(
    initialPath: String? = null,
    selectionMode: SelectionMode = SelectionMode.FILE,
    onConfirm: (String) -> Unit,
) {
    val viewModel = metroViewModel<FileChooserViewModel>()
    val state = viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(initialPath, selectionMode) {
        viewModel.dispatch(
            FileChooserIntent.Init(
                startPath = initialPath,
                selectionMode = selectionMode,
            )
        )
    }

    Content(
        uiState = state.value,
        dispatch = viewModel::dispatch,
        onConfirm = onConfirm,
    )
}

@Composable
private fun Content(
    modifier: Modifier = Modifier,
    uiState: FileUiState = FileUiState(),
    dispatch: (FileChooserIntent) -> Unit = {},
    onConfirm: (String) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.background)
            .padding(16.dp),
    ) {
        Header(selectionMode = uiState.selectionMode)

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Sidebar(
                places = uiState.places,
                drives = uiState.drives,
                selectedDrive = uiState.selectedDrive,
                currentDir = uiState.currentDir,
                dispatch = dispatch,
                modifier = Modifier.width(280.dp).fillMaxHeight(),
            )

            ViewerPanel(
                modifier = Modifier.weight(1f),
                currentDir = uiState.currentDir,
                showHidden = uiState.showHidden,
                onToggleHidden = { dispatch(FileChooserIntent.ToggleHidden) },
            ) {
                FileTree(
                    nodes = uiState.treeNodes,
                    currentDir = uiState.currentDir,
                    scrollToIndex = uiState.scrollToIndex,
                    onToggle = { dispatch(ToggleExpand(it)) },
                    onSelect = { dispatch(SelectPath(it)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        BottomSection(
            selectedPath = uiState.displaySelectedPath,
            canConfirm = uiState.canConfirm,
            confirmLabel = when (uiState.selectionMode) {
                SelectionMode.FILE -> "Select file"
                SelectionMode.DIRECTORY -> "Select folder"
            },
            onConfirm = { onConfirm(uiState.confirmPath) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Header(
    selectionMode: SelectionMode,
    modifier: Modifier = Modifier,
) {
    val title = when (selectionMode) {
        SelectionMode.FILE -> "Select a file"
        SelectionMode.DIRECTORY -> "Select a directory"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun ViewerPanel(
    modifier: Modifier = Modifier,
    currentDir: String,
    showHidden: Boolean,
    onToggleHidden: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val scrollState = rememberScrollState()
            Text(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                text = currentDir.ifEmpty { "No folder selected" },
                fontSize = 12.sp,
                color = if (currentDir.isNotEmpty()) {
                    SimAnalyzerTheme.material.primary.copy(alpha = 0.95f)
                } else {
                    SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.5f)
                },
                maxLines = 1,
            )

            Spacer(Modifier.width(8.dp))

            TextButton(onClick = onToggleHidden) {
                Text(
                    text = if (showHidden) "Hidden: ON" else "Hidden: OFF",
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun BottomSection(
    selectedPath: String,
    canConfirm: Boolean,
    confirmLabel: String,
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BaseTextField(
            value = selectedPath,
            placeholder = "No selection",
            leadingIcon = Icons.Filled.Folder,
            readOnly = true,
            modifier = Modifier.weight(1f),
        )

        Button(
            onClick = onConfirm,
            enabled = canConfirm,
            modifier = Modifier.height(36.dp),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SimAnalyzerTheme.material.primary,
            ),
        ) {
            Text(confirmLabel, fontSize = 12.sp)
        }
    }
}

@Composable
private fun BaseTextField(
    value: String,
    onValueChange: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    textStyle: TextStyle = TextStyle(fontSize = 12.sp),
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = true,
        textStyle = textStyle.copy(
            color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.85f),
        ),
        modifier = modifier
    )
}

@Preview
@Composable
private fun ChooserPreview() {
    SimAnalyzerTheme {
        Content(uiState = FileUiState())
    }
}