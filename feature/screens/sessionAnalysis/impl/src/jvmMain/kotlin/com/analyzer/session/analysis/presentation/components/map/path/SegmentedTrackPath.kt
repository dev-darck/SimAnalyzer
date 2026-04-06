package com.analyzer.session.analysis.presentation.components.map.path

import androidx.compose.ui.geometry.Offset

internal data class SegmentedTrackPath(val segments: List<List<Offset>>, val closeLoop: Boolean)
