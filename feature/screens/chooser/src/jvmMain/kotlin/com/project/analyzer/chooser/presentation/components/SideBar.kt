package com.project.analyzer.chooser.presentation.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.analyzer.chooser.domain.model.File
import com.project.analyzer.chooser.presentation.FileChooserIntent
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick

@Composable
internal fun Sidebar(
    places: List<File>,
    drives: List<File>,
    currentDir: String,
    selectedDrive: String,
    dispatch: (FileChooserIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SidebarPanel(title = "Quick access", modifier = Modifier.weight(weight = 0.45f)) {
            PlacesList(
                places = places,
                selectedPath = currentDir,
                onPlaceClick = { dispatch(FileChooserIntent.ClickPlace(it)) },
            )
        }

        SidebarPanel(title = "This PC", modifier = Modifier.weight(weight = 0.55f)) {
            DrivesList(
                drives = drives,
                selectedDrive = selectedDrive,
                onDriveClick = { dispatch(FileChooserIntent.OpenDirectory(it)) },
            )
        }
    }
}

@Composable
private fun SidebarPanel(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .padding(12.dp),
    ) {
        Text(
            text = title,
            color = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.75f),
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun PlacesList(places: List<File>, selectedPath: String, onPlaceClick: (File) -> Unit) {
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(items = places, key = { it.path }) { place ->
                SidebarItem(
                    text = place.label,
                    leadingIcon = Icons.Filled.Folder,
                    isSelected = place.path == selectedPath,
                    onClick = { onPlaceClick(place) },
                )
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(listState),
        )
    }
}

@Composable
private fun DrivesList(drives: List<File>, selectedDrive: String, onDriveClick: (File) -> Unit) {
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(items = drives) { drive ->
                val backgroundColor = if (drive.path == selectedDrive) {
                    SimAnalyzerTheme.material.primary.copy(alpha = 0.14f)
                } else {
                    SimAnalyzerTheme.material.surface
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(backgroundColor)
                        .onClick(onClick = { onDriveClick(drive) })
                        .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.75f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = drive.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(listState),
        )
    }
}

@Composable
private fun SidebarItem(text: String, leadingIcon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) {
        SimAnalyzerTheme.material.primary.copy(alpha = 0.14f)
    } else {
        SimAnalyzerTheme.material.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .onClick(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.75f),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.85f),
        )
    }
}
