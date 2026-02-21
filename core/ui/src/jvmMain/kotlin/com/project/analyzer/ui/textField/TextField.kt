@file:OptIn(ExperimentalStdlibApi::class)

package com.project.analyzer.ui.textField

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
public fun TextField(
    value: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    onValueChange: (String) -> Unit = {},
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable (() -> Unit))? = null,
    onClick: (() -> Unit) = {},
    valueSanitizer: (String) -> String = { it },
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = TextStyle(fontSize = 12.sp),
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val borderColor = when {
        !enabled -> SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.25f)
        isError -> SimAnalyzerTheme.material.error
        focused -> SimAnalyzerTheme.material.primary.copy(alpha = 0.75f)
        else -> SimAnalyzerTheme.material.primary.copy(alpha = 0.45f)
    }

    val bgColor =
        if (enabled) {
            SimAnalyzerTheme.material.background
        } else {
            SimAnalyzerTheme.material.background.copy(alpha = 0.6f)
        }

    val clickable =
        if (enabled && readOnly) {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { onClick() }
        } else {
            Modifier
        }

    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .then(clickable)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.70f),
            )
            Spacer(Modifier.width(12.dp))
        }

        BasicTextField(
            value = value,
            onValueChange = { onValueChange(valueSanitizer(it)) },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            textStyle = textStyle.copy(color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.85f)),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(SimAnalyzerTheme.material.primary),
            interactionSource = interactionSource,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth()) {
                    if (value.isBlank() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            color = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.55f),
                            style = textStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
            },
        )

        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}
