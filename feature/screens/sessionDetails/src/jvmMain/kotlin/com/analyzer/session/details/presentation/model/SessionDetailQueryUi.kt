package com.analyzer.session.details.presentation.model

internal object SessionDetailFilterIdsUi {
    const val SortLap = "lap"
    const val SortLapDesc = "lap_desc"
    const val SortBest = "best"
    const val SortTotalDesc = "total_desc"
    const val SortS1 = "s1"
    const val SortS1Desc = "s1_desc"
    const val SortS2 = "s2"
    const val SortS2Desc = "s2_desc"
    const val SortS3 = "s3"
    const val SortS3Desc = "s3_desc"
    const val SortIncidents = "incidents"
    const val SortIncidentsDesc = "incidents_desc"
    const val SortDelta = "delta"
    const val SortDeltaDesc = "delta_desc"
    const val SortStatus = "status"
    const val SortStatusDesc = "status_desc"

    const val ShowAll = "all"
    const val ShowValid = "valid"
    const val ShowInvalid = "invalid"
    const val ShowPit = "pit"

    const val SessionTypeAuto = "__auto__"
    const val SessionTypeAll = "all_session_types"
}

internal data class SessionDetailQueryUi(
    val sortId: String = SessionDetailFilterIdsUi.SortLap,
    val showId: String = SessionDetailFilterIdsUi.ShowAll,
    val sessionTypeId: String = SessionDetailFilterIdsUi.SessionTypeAuto,
    val page: Int = 1,
)
