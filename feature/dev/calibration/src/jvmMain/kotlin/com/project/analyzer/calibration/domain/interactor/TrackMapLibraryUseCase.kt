package com.project.analyzer.calibration.domain.interactor

import com.project.analyzer.calibration.presentation.trackmap.TrackMapLibraryItem

interface TrackMapLibraryUseCase {

    suspend fun loadItems(): List<TrackMapLibraryItem>
}
