package com.project.analyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme

public enum class SimAnalyzerButtonVariant {
    Primary,
    Secondary,
    Outline,
    Text,
}

public enum class SimAnalyzerButtonSize {
    Default,
    Compact,
}

@Composable
public fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: SimAnalyzerButtonVariant = SimAnalyzerButtonVariant.Primary,
    size: SimAnalyzerButtonSize = SimAnalyzerButtonSize.Default,
    content: @Composable RowScope.() -> Unit,
) {
    val palette = buttonPalette(variant)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val textStyle = when (size) {
        SimAnalyzerButtonSize.Default -> SimAnalyzerTheme.typography.labelLarge
        SimAnalyzerButtonSize.Compact -> SimAnalyzerTheme.typography.labelMedium
    }
    val contentPadding = when (size) {
        SimAnalyzerButtonSize.Default -> PaddingValues(horizontal = 14.dp, vertical = 9.dp)
        SimAnalyzerButtonSize.Compact -> PaddingValues(horizontal = 12.dp, vertical = 7.dp)
    }
    val minHeight = when (size) {
        SimAnalyzerButtonSize.Default -> 36.dp
        SimAnalyzerButtonSize.Compact -> 30.dp
    }
    val shape = SimAnalyzerTheme.corners.item
    val buttonModifier = modifier.defaultMinSize(minHeight = minHeight)
    val containerColor = when {
        !enabled -> palette.disabledContainerColor
        isPressed -> palette.pressedContainerColor
        isHovered -> palette.hoverContainerColor
        else -> palette.containerColor
    }
    val contentColor = if (enabled) palette.contentColor else palette.disabledContentColor
    val borderColor = when {
        !enabled -> palette.disabledBorderColor
        isPressed -> palette.pressedBorderColor
        isHovered -> palette.hoverBorderColor
        else -> palette.borderColor
    }

    Row(
        modifier = buttonModifier
            .clip(shape)
            .background(containerColor)
            .then(
                if (borderColor != null) {
                    Modifier.border(width = 1.dp, color = borderColor, shape = shape)
                } else {
                    Modifier
                },
            )
            .then(
                if (enabled) {
                    Modifier.hoverable(interactionSource = interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick,
                        )
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(value = textStyle) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }
}

@Composable
public fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: SimAnalyzerButtonVariant = SimAnalyzerButtonVariant.Primary,
    size: SimAnalyzerButtonSize = SimAnalyzerButtonSize.Default,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val iconSize = when (size) {
        SimAnalyzerButtonSize.Default -> 16.dp
        SimAnalyzerButtonSize.Compact -> 14.dp
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        variant = variant,
        size = size,
    ) {
        if (leadingIcon != null) {
            androidx.compose.material3.Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
        }

        Text(text = text)

        if (trailingIcon != null) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
            androidx.compose.material3.Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Immutable
private data class SimAnalyzerButtonPalette(
    val containerColor: Color,
    val hoverContainerColor: Color,
    val pressedContainerColor: Color,
    val contentColor: Color,
    val borderColor: Color? = null,
    val hoverBorderColor: Color? = borderColor,
    val pressedBorderColor: Color? = borderColor,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
    val disabledBorderColor: Color? = borderColor,
)

@Composable
private fun buttonPalette(variant: SimAnalyzerButtonVariant): SimAnalyzerButtonPalette {
    val disabledContainer = SimAnalyzerTheme.chrome.fillDisabled
    val disabledContent = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.7f)
    val mutedSurface = SimAnalyzerTheme.chrome.fillMuted

    return when (variant) {
        SimAnalyzerButtonVariant.Primary -> SimAnalyzerButtonPalette(
            containerColor = lerp(
                start = SimAnalyzerTheme.material.primary,
                stop = SimAnalyzerTheme.material.primaryContainer,
                fraction = 0.16f,
            ),
            hoverContainerColor = lerp(
                start = SimAnalyzerTheme.material.primary,
                stop = SimAnalyzerTheme.material.primaryContainer,
                fraction = 0.26f,
            ),
            pressedContainerColor = lerp(
                start = SimAnalyzerTheme.material.primary,
                stop = SimAnalyzerTheme.material.surface,
                fraction = 0.10f,
            ),
            contentColor = SimAnalyzerTheme.material.onPrimary,
            borderColor = SimAnalyzerTheme.material.primary.copy(alpha = 0.32f),
            hoverBorderColor = SimAnalyzerTheme.material.primary.copy(alpha = 0.42f),
            pressedBorderColor = SimAnalyzerTheme.material.primary.copy(alpha = 0.52f),
            disabledContainerColor = disabledContainer,
            disabledContentColor = disabledContent,
            disabledBorderColor = SimAnalyzerTheme.chrome.borderSubtle,
        )

        SimAnalyzerButtonVariant.Secondary -> SimAnalyzerButtonPalette(
            containerColor = SimAnalyzerTheme.material.primary.copy(alpha = 0.14f),
            hoverContainerColor = SimAnalyzerTheme.material.primary.copy(alpha = 0.18f),
            pressedContainerColor = SimAnalyzerTheme.material.primary.copy(alpha = 0.22f),
            contentColor = SimAnalyzerTheme.material.primary,
            borderColor = SimAnalyzerTheme.material.primary.copy(alpha = 0.24f),
            hoverBorderColor = SimAnalyzerTheme.material.primary.copy(alpha = 0.34f),
            pressedBorderColor = SimAnalyzerTheme.material.primary.copy(alpha = 0.44f),
            disabledContainerColor = disabledContainer,
            disabledContentColor = disabledContent,
            disabledBorderColor = SimAnalyzerTheme.chrome.borderSubtle,
        )

        SimAnalyzerButtonVariant.Outline -> SimAnalyzerButtonPalette(
            containerColor = mutedSurface,
            hoverContainerColor = SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.22f),
            pressedContainerColor = SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.30f),
            contentColor = SimAnalyzerTheme.material.onSurface,
            borderColor = SimAnalyzerTheme.chrome.borderStrong,
            hoverBorderColor = SimAnalyzerTheme.chrome.borderEmphasis,
            pressedBorderColor = SimAnalyzerTheme.chrome.borderSecondary,
            disabledContainerColor = disabledContainer,
            disabledContentColor = disabledContent,
            disabledBorderColor = SimAnalyzerTheme.chrome.borderSubtle,
        )

        SimAnalyzerButtonVariant.Text -> SimAnalyzerButtonPalette(
            containerColor = Color.Transparent,
            hoverContainerColor = SimAnalyzerTheme.material.primary.copy(alpha = 0.10f),
            pressedContainerColor = SimAnalyzerTheme.material.primary.copy(alpha = 0.16f),
            contentColor = SimAnalyzerTheme.material.primary,
            borderColor = null,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = disabledContent,
            disabledBorderColor = null,
        )
    }
}

@Preview
@Composable
private fun SimAnalyzerButtonPreview() {
    SimAnalyzerTheme {
        Row(
            modifier = Modifier
                .background(SimAnalyzerTheme.material.background)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(text = "Primary", onClick = {})
            Button(
                text = "Secondary",
                onClick = {},
                variant = SimAnalyzerButtonVariant.Secondary,
            )
            Button(
                text = "Outline",
                onClick = {},
                variant = SimAnalyzerButtonVariant.Outline,
            )
        }
    }
}
