package com.analyzer.session.data.repository

public data class RecordedSessionListRequest(
    val gameId: String? = null,
    val trackId: String? = null,
    val carId: String? = null,
    val dateId: String? = null,
    val searchQuery: String = "",
    val sort: RecordedSessionSummarySort = RecordedSessionSummarySort.StartedAtDesc,
    val page: Int = 1,
    val pageSize: Int = 8,
)

public enum class RecordedSessionSummarySort {
    StartedAtDesc,
    StartedAtAsc,
    GameAsc,
    GameDesc,
    TrackAsc,
    TrackDesc,
    CarAsc,
    CarDesc,
    LapCountAsc,
    LapCountDesc,
    BestLapAsc,
    BestLapDesc,
}

public data class RecordedSessionDetailRequest(
    val sort: RecordedSessionLapSort = RecordedSessionLapSort.LapAsc,
    val show: RecordedSessionLapShow = RecordedSessionLapShow.All,
    val sessionTypeId: String? = null,
    val autoSelectSessionType: Boolean = false,
    val page: Int = 1,
    val pageSize: Int = 10,
)

public enum class RecordedSessionLapSort {
    LapAsc,
    LapDesc,
    TotalTimeAsc,
    TotalTimeDesc,
    Sector1Asc,
    Sector1Desc,
    Sector2Asc,
    Sector2Desc,
    Sector3Asc,
    Sector3Desc,
    IncidentsAsc,
    IncidentsDesc,
    DeltaAsc,
    DeltaDesc,
    StatusAsc,
    StatusDesc,
}

public enum class RecordedSessionLapShow {
    All,
    Valid,
    Invalid,
    Pit,
}
