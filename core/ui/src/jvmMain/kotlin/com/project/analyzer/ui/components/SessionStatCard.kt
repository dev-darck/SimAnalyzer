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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.core.ui.Res.Res
import com.project.analyzer.core.ui.Res.session_stat_card_preview_title
import com.project.analyzer.core.ui.Res.session_stat_card_preview_value
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource

public data class SessionStatCardUi(val titleLabel: String, val value: String)

public fun sessionStatCardUi(title: String, value: String): SessionStatCardUi = SessionStatCardUi(
    titleLabel = title.uppercase(),
    value = value,
)

@Composable
public fun SessionStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    SessionStatCard(
        state = sessionStatCardUi(title = title, value = value),
        modifier = modifier,
    )
}

@Composable
public fun SessionStatCard(state: SessionStatCardUi, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .border(
                1.dp,
                SimAnalyzerTheme.chrome.borderStrong,
                SimAnalyzerTheme.shapes.large,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = state.titleLabel,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = state.value,
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
private fun SessionStatCardPreview() {
    SimAnalyzerTheme {
        SessionStatCard(
            title = stringResource(Res.string.session_stat_card_preview_title),
            value = stringResource(Res.string.session_stat_card_preview_value),
        )
    }
}
