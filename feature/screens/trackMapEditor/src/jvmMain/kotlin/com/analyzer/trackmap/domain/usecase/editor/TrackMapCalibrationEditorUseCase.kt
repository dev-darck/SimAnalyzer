package com.analyzer.trackmap.domain.usecase.editor

import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorContent
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorSnapshot
import com.analyzer.trackmap.domain.model.TrackMapLibraryItem

interface TrackMapCalibrationEditorUseCase {

    suspend fun load(gameId: String, trackId: String, layoutId: String?): TrackMapCalibrationEditorContent?

    suspend fun save(
        item: TrackMapLibraryItem,
        snapshot: TrackMapCalibrationEditorSnapshot,
    ): TrackMapCalibrationEditorSnapshot
}
