package com.analyzer.session.details.domain.model

const val SESSION_DETAIL_SORT_LAP = "lap"
const val SESSION_DETAIL_SORT_LAP_DESC = "lap_desc"
const val SESSION_DETAIL_SORT_BEST = "best"
const val SESSION_DETAIL_SORT_TOTAL_DESC = "total_desc"
const val SESSION_DETAIL_SORT_S1 = "s1"
const val SESSION_DETAIL_SORT_S1_DESC = "s1_desc"
const val SESSION_DETAIL_SORT_S2 = "s2"
const val SESSION_DETAIL_SORT_S2_DESC = "s2_desc"
const val SESSION_DETAIL_SORT_S3 = "s3"
const val SESSION_DETAIL_SORT_S3_DESC = "s3_desc"
const val SESSION_DETAIL_SORT_INCIDENTS = "incidents"
const val SESSION_DETAIL_SORT_INCIDENTS_DESC = "incidents_desc"
const val SESSION_DETAIL_SORT_DELTA = "delta"
const val SESSION_DETAIL_SORT_DELTA_DESC = "delta_desc"
const val SESSION_DETAIL_SORT_STATUS = "status"
const val SESSION_DETAIL_SORT_STATUS_DESC = "status_desc"
const val SESSION_DETAIL_SHOW_ALL = "all"
const val SESSION_DETAIL_SHOW_VALID = "valid"
const val SESSION_DETAIL_SHOW_INVALID = "invalid"
const val SESSION_DETAIL_SHOW_PIT = "pit"
const val SESSION_DETAIL_TYPE_AUTO = "__auto__"
const val SESSION_DETAIL_TYPE_ALL = "all_session_types"

data class SessionDetailQuery(
    val sortId: String = SESSION_DETAIL_SORT_LAP,
    val showId: String = SESSION_DETAIL_SHOW_ALL,
    val sessionTypeId: String = SESSION_DETAIL_TYPE_AUTO,
    val page: Int = 1,
)

data class SessionDetailDomainHeader(
    val subtitle: String = "",
    val sessionTypeLabel: String = "",
    val airTempLabel: String = "--°C",
    val trackTempLabel: String = "--°C",
    val carLabel: String = "",
    val trackLabel: String = "",
    val gameId: String = "",
    val trackId: String? = null,
    val layoutId: String? = null,
    val carModel: String? = null,
    val carId: Int? = null,
    val savedCarId: String? = null,
    val thumbnailPath: String? = null,
)

data class SessionDetailDomainStats(
    val bestLapLabel: String = "0:00.000",
    val averageLapLabel: String = "0:00.000",
    val incidentsCount: Int = 0,
)

enum class SessionLapDomainStatus {
    Clean,
    OutLap,
    Dirty,
    BestLap,
    Invalid,
    PitIn,
}

data class SessionLapDomainItem(
    val segmentId: Long,
    val lapNumber: Int,
    val lapLabel: String,
    val sessionTypeId: String,
    val sessionTypeLabel: String,
    val totalTimeMs: Int?,
    val totalTime: String,
    val s1Ms: Int?,
    val s1: String,
    val s2Ms: Int?,
    val s2: String,
    val s3Ms: Int?,
    val s3: String,
    val incidentsCount: Int,
    val incidents: String,
    val deltaMs: Int?,
    val delta: String,
    val deltaIsPositive: Boolean,
    val status: SessionLapDomainStatus,
)

data class SessionDetailSessionTypeOption(val id: String, val label: String)

data class SessionDetailPage(
    val header: SessionDetailDomainHeader = SessionDetailDomainHeader(),
    val stats: SessionDetailDomainStats = SessionDetailDomainStats(),
    val sessionTypeOptions: List<SessionDetailSessionTypeOption> = emptyList(),
    val defaultSessionTypeId: String = SESSION_DETAIL_TYPE_ALL,
    val laps: List<SessionLapDomainItem> = emptyList(),
    val page: Int,
    val pageCount: Int,
    val error: String?,
)

data class SessionDetailPageResult(val query: SessionDetailQuery, val page: SessionDetailPage)
