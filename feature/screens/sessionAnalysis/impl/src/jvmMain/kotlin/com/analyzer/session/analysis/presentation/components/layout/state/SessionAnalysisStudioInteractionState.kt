package com.analyzer.session.analysis.presentation.components.layout.state

import androidx.compose.runtime.Stable
import com.analyzer.session.analysis.presentation.components.inspector.model.SessionAnalysisInspectorTab
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi

@Stable
internal class SessionAnalysisStudioInteractionState(
    val inspectorTab: SessionAnalysisInspectorTab,
    val activePoint: SessionAnalysisComparisonPointUi?,
    val activeSample: SessionAnalysisSampleUi?,
    val selectionLocked: Boolean,
    val focusMode: Boolean,
    val heroCollapsed: Boolean,
    val cursorFraction: Float?,
    val cursorFrameId: Long?,
    val onInspectorTabSelected: (SessionAnalysisInspectorTab) -> Unit,
    val onFocusModeToggle: () -> Unit,
    val onHeroCollapseToggle: () -> Unit,
    val onGraphHoverFraction: (Float?) -> Unit,
    val onGraphPressFraction: (Float?) -> Unit,
    val onMapHover: (Float?, Long?) -> Unit,
    val onMapPress: (Float?, Long?) -> Unit,
)
