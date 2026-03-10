package com.analyzer.trackmap.presentation

internal fun buildTrackMapCalibrationEditorMapKey(gameId: String, trackId: String, layoutId: String?): String =
    buildString {
        append(gameId)
        append('|')
        append(trackId)
        append('|')
        append(layoutId.orEmpty())
    }
