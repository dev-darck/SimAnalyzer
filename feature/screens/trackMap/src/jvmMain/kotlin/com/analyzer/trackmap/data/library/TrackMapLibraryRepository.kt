package com.analyzer.trackmap.data.library

import com.analyzer.trackmap.domain.model.TrackMapLibraryItem

interface TrackMapLibraryRepository {

    suspend fun loadItems(): List<TrackMapLibraryItem>

    suspend fun loadItem(gameId: String, trackId: String, layoutId: String? = null): TrackMapLibraryItem?
}
