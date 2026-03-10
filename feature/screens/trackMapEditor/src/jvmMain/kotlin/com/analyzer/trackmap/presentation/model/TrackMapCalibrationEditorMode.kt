package com.analyzer.trackmap.presentation.model

internal enum class TrackMapCalibrationEditorMode(val title: String, val description: String) {
    Move(
        title = "Move",
        description = "Move the gate center. The point snaps to the nearest track point on release.",
    ),
    Direction(
        title = "Direction",
        description = "Rotate the crossing direction by dragging only along the track centerline.",
    ),
    Width(
        title = "Width",
        description = "Resize the gate width in snapped steps instead of pixel-perfect dragging.",
    ),
}
