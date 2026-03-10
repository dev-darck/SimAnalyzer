package com.analyzer.session.domain.model

data class SessionListDomainStats(
    val totalDistanceKm: Double = 0.0,
    val sessionsCount: Int = 0,
    val incidentsCount: Int = 0,
    val favoriteCar: String = "-",
)
