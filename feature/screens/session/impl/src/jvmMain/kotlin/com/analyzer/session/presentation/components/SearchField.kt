package com.analyzer.session.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick

private val SearchFieldShape = SimAnalyzerTheme.corners.field
private val SearchFieldMinHeight = 38.dp

@Composable
internal fun SearchField(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val containerColor = SimAnalyzerTheme.material.secondaryContainer
    val contentColor = SimAnalyzerTheme.material.onSecondaryContainer
    val borderColor = if (focused) {
        SimAnalyzerTheme.chrome.borderInteractiveStrong
    } else {
        SimAnalyzerTheme.chrome.borderSecondary
    }

    Row(
        modifier = modifier
            .heightIn(min = SearchFieldMinHeight)
            .clip(SearchFieldShape)
            .background(containerColor)
            .border(1.dp, borderColor, SearchFieldShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.requiredSize(18.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = SimAnalyzerTheme.typography.labelMedium.copy(color = contentColor),
            cursorBrush = SolidColor(SimAnalyzerTheme.material.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            color = contentColor.copy(alpha = 0.65f),
                            style = SimAnalyzerTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (value.isNotBlank()) {
            Spacer(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .requiredSize(20.dp)
                    .onClick { onValueChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.72f),
                    modifier = Modifier.requiredSize(14.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun SearchFieldPreview() {
    SimAnalyzerTheme {
        SearchField(
            value = "Spa",
            placeholder = "Search sessions...",
            modifier = Modifier.width(300.dp),
            onValueChange = {},
        )
    }
}
