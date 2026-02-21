package com.analyzer.session.details.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
internal fun InfoChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.2f))
            .border(1.dp, SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            fontSize = 11.sp,
        )
    }
}
