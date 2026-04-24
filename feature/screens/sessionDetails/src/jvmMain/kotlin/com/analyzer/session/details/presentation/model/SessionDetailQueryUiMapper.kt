package com.analyzer.session.details.presentation.model

import com.analyzer.session.details.domain.model.SessionDetailQuery

internal fun SessionDetailQueryUi.toDomain(): SessionDetailQuery = SessionDetailQuery(
    sortId = sortId,
    showId = showId,
    sessionTypeId = sessionTypeId,
    page = page,
)

internal fun SessionDetailQuery.toUi(): SessionDetailQueryUi = SessionDetailQueryUi(
    sortId = sortId,
    showId = showId,
    sessionTypeId = sessionTypeId,
    page = page,
)
