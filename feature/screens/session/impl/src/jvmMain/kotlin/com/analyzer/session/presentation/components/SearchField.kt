package com.analyzer.session.presentation.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.analyzer.ui.textField.TextField

@Composable
internal fun SearchField(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    TextField(
        value = value,
        placeholder = placeholder,
        onValueChange = onValueChange,
        leadingIcon = Icons.Filled.Search,
        modifier = modifier.height(36.dp),
    )
}
