package com.analyzer.trackmap.presentation.mapper

import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.analyzer.trackmap.domain.model.mapKey
import com.analyzer.trackmap.presentation.model.TrackMapLibraryCardUi
import com.analyzer.trackmap.presentation.model.TrackMapLibraryHeaderUi
import com.analyzer.trackmap.presentation.model.TrackMapLibraryPointsPreviewUi
import com.analyzer.trackmap.presentation.model.TrackMapLibraryStatsUi
import com.analyzer.trackmap.presentation.model.TrackMapPreviewUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val MAX_POINTS_PREVIEW_LINES = 200
private const val MAX_CARD_TRACK_POINTS = 360
private const val MAX_CARD_PIT_POINTS = 120
private const val MAX_CARD_MARKER_POINTS = 24
private val TrackMapCreatedAtFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

internal fun TrackMapLibraryItem.toTrackMapLibraryCardUi(): TrackMapLibraryCardUi {
    val preview = toTrackMapPreviewUi()
    val cardPreview = preview.toLibraryCardPreview()
    val gameLabel = map.gameId.ifBlank { "unknown" }
    val pointsPreview = preview.points
        .take(MAX_POINTS_PREVIEW_LINES)
        .toImmutableList()

    return TrackMapLibraryCardUi(
        mapKey = mapKey(),
        gameId = map.gameId,
        trackId = map.trackId,
        layoutId = map.layoutId,
        header = TrackMapLibraryHeaderUi(
            title = map.trackName.ifBlank { map.trackId },
            subtitle = "$gameLabel · ${map.trackId}",
            layoutLabel = map.layoutId?.takeIf(String::isNotBlank),
        ),
        stats = TrackMapLibraryStatsUi(
            pointCount = points.size,
            distanceMeters = distanceMeters,
            averageTrackWidthMeters = preview.averageTrackWidthMeters,
            pitPointCount = pitPoints.size,
            createdAtLabel = formatEpoch(map.createdAtEpochMs),
        ),
        preview = cardPreview,
        pointsPreview = TrackMapLibraryPointsPreviewUi(
            points = pointsPreview,
            hiddenCount = (points.size - pointsPreview.size).coerceAtLeast(0),
        ),
    )
}

private fun formatEpoch(epochMs: Long): String = Instant.ofEpochMilli(epochMs)
    .atZone(ZoneId.systemDefault())
    .format(TrackMapCreatedAtFormatter)

private fun TrackMapPreviewUi.toLibraryCardPreview(): TrackMapPreviewUi = copy(
    points = points.downsampleTo(MAX_CARD_TRACK_POINTS),
    leftWidthsMeters = persistentListOf(),
    rightWidthsMeters = persistentListOf(),
    pitPoints = pitPoints.downsampleTo(MAX_CARD_PIT_POINTS),
    sectorMarkerPositions = sectorMarkerPositions.downsampleTo(MAX_CARD_MARKER_POINTS),
)

private fun <T> ImmutableList<T>.downsampleTo(maxSize: Int): ImmutableList<T> {
    if (size <= maxSize) return this
    if (maxSize <= 0) return persistentListOf()
    if (maxSize == 1) return persistentListOf(first())

    val result = ArrayList<T>(maxSize)
    val lastIndex = size - 1
    for (sampleIndex in 0 until maxSize) {
        val sourceIndex = (sampleIndex.toLong() * lastIndex / (maxSize - 1)).toInt()
        result += this[sourceIndex]
    }
    return result.toImmutableList()
}
