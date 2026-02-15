package com.analyzer.session.presentation.model

data class SessionStatsUi(
    val totalDistanceLabel: String = "0.000",
    val sessionsCount: Int = 0,
    val incidentsCount: Int = 0,
    val favoriteCar: String = "-",
)
