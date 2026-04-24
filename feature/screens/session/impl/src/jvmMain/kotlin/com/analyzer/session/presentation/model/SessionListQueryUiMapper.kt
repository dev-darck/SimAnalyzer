package com.analyzer.session.presentation.model

import com.analyzer.session.domain.model.SessionListQuery

internal fun SessionListQueryUi.toDomain(): SessionListQuery = SessionListQuery(
    gameId = gameId,
    trackId = trackId,
    carId = carId,
    dateId = dateId,
    sortId = sortId,
    searchQuery = searchQuery,
    page = page,
)

internal fun SessionListQuery.toUi(): SessionListQueryUi = SessionListQueryUi(
    gameId = gameId,
    trackId = trackId,
    carId = carId,
    dateId = dateId,
    sortId = sortId,
    searchQuery = searchQuery,
    page = page,
)
