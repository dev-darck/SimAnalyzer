package com.analyzer.trackmap.domain.model

fun TrackMapLibraryItem.mapKey(): String = buildTrackMapLibraryItemKey(
    gameId = map.gameId,
    trackId = map.trackId,
    layoutId = map.layoutId,
)

fun buildTrackMapLibraryItemKey(gameId: String, trackId: String, layoutId: String?): String = buildString {
    append(gameId.trim().lowercase())
    append('|')
    append(trackId.trim())
    append('|')
    append(layoutId.orEmpty().trim().lowercase())
}
