package com.analyzer.trackmap.domain.usecase

import com.analyzer.trackmap.domain.model.TrackMapLibraryItem

interface TrackMapLibraryUseCase {
    suspend fun loadItems(): List<TrackMapLibraryItem>

    suspend fun loadItem(gameId: String, trackId: String, layoutId: String?): TrackMapLibraryItem?
    fun rememberSelection(item: TrackMapLibraryItem)
}
