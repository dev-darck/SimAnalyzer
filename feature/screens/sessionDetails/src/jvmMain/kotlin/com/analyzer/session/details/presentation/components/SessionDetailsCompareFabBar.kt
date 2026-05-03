package com.analyzer.session.details.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.analyzer.session.details.presentation.model.SessionDetailCompareLapUi
import com.project.analyzer.feature.screens.sessionDetails.Res.Res
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_compare
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_compare_cancel
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_compare_cancel_short
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_compare_confirm
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_action_compare_confirm_short
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionDetailsWorkspaceFabBar(
    isCompareSelectionMode: Boolean,
    selectedCompareLaps: ImmutableList<SessionDetailCompareLapUi>,
    compareConfirmEnabled: Boolean,
    compareStartEnabled: Boolean,
    actionsEnabled: Boolean,
    modifier: Modifier = Modifier,
    onStartCompare: () -> Unit = {},
    onCancelCompare: () -> Unit = {},
    onConfirmCompare: () -> Unit = {},
    onOpenCompareSessionPicker: () -> Unit = {},
    onShareResults: () -> Unit = {},
) {
    val compareFabDescription = stringResource(Res.string.session_details_action_compare)

    AnimatedContent(
        targetState = isCompareSelectionMode,
        modifier = modifier,
        transitionSpec = {
            (
                fadeIn() + scaleIn(initialScale = 0.96f) + slideInHorizontally(initialOffsetX = { it / 5 })
                ) togetherWith (
                fadeOut() + scaleOut(targetScale = 0.96f) + slideOutHorizontally(targetOffsetX = { it / 5 })
                )
        },
        label = "session-details-compare-fab",
    ) { compareSelectionMode ->
        if (compareSelectionMode) {
            SessionDetailsCompareSelectionDock(
                compareConfirmEnabled = compareConfirmEnabled,
                title = compareFabDescription,
                statusLabel = compareFabLabel(
                    isCompareSelectionMode = true,
                    selectedCompareLaps = selectedCompareLaps,
                ),
                cancelLabel = stringResource(Res.string.session_details_action_compare_cancel_short),
                confirmLabel = stringResource(Res.string.session_details_action_compare_confirm_short),
                cancelDescription = stringResource(Res.string.session_details_action_compare_cancel),
                confirmDescription = stringResource(Res.string.session_details_action_compare_confirm),
                onCancelCompare = onCancelCompare,
                onConfirmCompare = onConfirmCompare,
            )
        } else {
            SessionDetailsWorkspaceSpeedDial(
                selectedCompareLaps = selectedCompareLaps,
                compareStartEnabled = compareStartEnabled,
                actionsEnabled = actionsEnabled,
                compareFabDescription = compareFabDescription,
                onStartCompare = onStartCompare,
                onOpenCompareSessionPicker = onOpenCompareSessionPicker,
                onShareResults = onShareResults,
            )
        }
    }
}
