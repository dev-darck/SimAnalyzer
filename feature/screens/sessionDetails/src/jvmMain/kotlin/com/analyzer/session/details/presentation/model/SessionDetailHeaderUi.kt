package com.analyzer.session.details.presentation.model

data class SessionDetailHeaderUi(
    val subtitle: String = "",
    val sessionTypeLabel: String = "",
    val airTempLabel: String = "--°C",
    val trackTempLabel: String = "--°C",
    val carLabel: String = "",
    val trackLabel: String = "",
    val savedCarId: String? = null,
    val thumbnailPath: String? = null,
)
