package com.analyzer.session.analysis.presentation.components.map.support

import androidx.compose.ui.geometry.Offset

internal data class SectorCenterProjection(val point: Offset, val direction: Offset, val distanceToSource: Float)
