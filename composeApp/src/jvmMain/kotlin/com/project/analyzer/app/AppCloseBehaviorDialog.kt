package com.project.analyzer.app

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.analyzer.composeApp.Res.Res
import com.project.analyzer.composeApp.Res.app_close_behavior_exit
import com.project.analyzer.composeApp.Res.app_close_behavior_minimize_to_tray
import com.project.analyzer.composeApp.Res.app_close_behavior_remember
import com.project.analyzer.composeApp.Res.app_close_behavior_supporting
import com.project.analyzer.composeApp.Res.app_close_behavior_title
import com.project.analyzer.ui.components.InfoDialog
import com.project.analyzer.ui.components.SimAnalyzerButtonSize
import com.project.analyzer.ui.components.SimAnalyzerButtonVariant
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AppCloseBehaviorDialog(
    rememberDecision: Boolean,
    onRememberDecisionChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    onExitClick: () -> Unit,
    onMinimizeToTrayClick: () -> Unit,
) {
    InfoDialog(
        modifier = Modifier.widthIn(max = 400.dp),
        title = stringResource(Res.string.app_close_behavior_title),
        checkboxText = stringResource(Res.string.app_close_behavior_remember),
        checked = rememberDecision,
        onCheckedChange = onRememberDecisionChange,
        supportingText = stringResource(Res.string.app_close_behavior_supporting),
        dismissButtonText = stringResource(Res.string.app_close_behavior_exit),
        confirmButtonText = stringResource(Res.string.app_close_behavior_minimize_to_tray),
        onDismissRequest = onDismissRequest,
        onDismissClick = onExitClick,
        onConfirmClick = onMinimizeToTrayClick,
        dismissButtonVariant = SimAnalyzerButtonVariant.Secondary,
        dismissButtonWidth = 176.dp,
        confirmButtonWidth = 176.dp,
        buttonHeight = 32.dp,
        buttonSize = SimAnalyzerButtonSize.Compact,
    )
}
