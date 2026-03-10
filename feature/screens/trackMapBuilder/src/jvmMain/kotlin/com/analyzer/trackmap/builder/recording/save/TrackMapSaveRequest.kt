package com.analyzer.trackmap.builder.recording.save

import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration

internal data class TrackMapSaveRequest(val payload: TrackMapSavePayload, val calibration: TrackCalibration?)
