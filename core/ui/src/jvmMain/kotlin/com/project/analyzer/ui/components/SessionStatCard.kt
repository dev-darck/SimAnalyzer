package com.project.analyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
public fun SessionStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .border(
                1.dp,
                SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.35f),
                SimAnalyzerTheme.shapes.large,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = title.uppercase(),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            color = SimAnalyzerTheme.material.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
