package com.analyzer.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.theme.ThemeMode
import com.project.analyzer.ui.icons.Dark
import com.project.analyzer.ui.icons.Light
import com.project.analyzer.ui.icons.System

@Composable
internal fun AppearanceBlock(
    selectedTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(shape = SimAnalyzerTheme.shapes.large)
            .background(color = SimAnalyzerTheme.material.surface)
            .padding(all = 16.dp)
    ) {
        Text(
            text = "Appearance",
            color = SimAnalyzerTheme.material.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Theme mode",
            color = SimAnalyzerTheme.material.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemeOption(
                icon = Icons.Filled.Light,
                label = "Light",
                isSelected = selectedTheme == ThemeMode.Light,
                onClick = { onThemeSelected(ThemeMode.Light) },
                modifier = Modifier.weight(1f)
            )
            ThemeOption(
                icon = Icons.Filled.Dark,
                label = "Dark",
                isSelected = selectedTheme == ThemeMode.Dark,
                onClick = { onThemeSelected(ThemeMode.Dark) },
                modifier = Modifier.weight(1f)
            )
            ThemeOption(
                icon = Icons.Filled.System,
                label = "System",
                isSelected = selectedTheme == ThemeMode.System,
                onClick = { onThemeSelected(ThemeMode.System) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemeOption(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        SimAnalyzerTheme.material.primary
    } else {
        SimAnalyzerTheme.material.surfaceVariant
    }

    val contentColor = if (isSelected) {
        SimAnalyzerTheme.material.onPrimary
    } else {
        SimAnalyzerTheme.material.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview
@Composable
private fun AppearanceBlockPreview() {
    SimAnalyzerTheme {
        AppearanceBlock(
            selectedTheme = ThemeMode.Dark,
            onThemeSelected = {}
        )
    }
}
