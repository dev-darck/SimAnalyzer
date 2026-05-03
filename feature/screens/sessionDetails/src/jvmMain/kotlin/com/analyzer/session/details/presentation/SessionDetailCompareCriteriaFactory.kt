package com.analyzer.session.details.presentation

import com.analyzer.session.details.domain.model.SessionDetailDomainHeader
import com.analyzer.session.details.domain.usecase.SessionDetailCompareCriteria
import java.util.Locale

internal fun SessionDetailDomainHeader.toCompareCriteria(sessionId: Long): SessionDetailCompareCriteria? {
    val normalizedGameId = gameId.normalizeSessionDetailIdentity() ?: return null
    val resolvedTrackId = trackId
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?: return null
    return SessionDetailCompareCriteria(
        currentSessionId = sessionId,
        gameId = normalizedGameId,
        gameLabel = normalizedGameId.toSessionDetailGameLabel(),
        trackId = resolvedTrackId,
        layoutId = layoutId.normalizeSessionDetailIdentity(),
        trackLabel = trackLabel,
        preferredCarIdentityKey = carId
            ?.takeIf { it > 0 }
            ?.toString()
            ?: carModel.normalizeSessionDetailIdentity(),
    )
}

private fun String?.normalizeSessionDetailIdentity(): String? = this
    ?.trim()
    ?.takeIf(String::isNotBlank)
    ?.lowercase(Locale.US)

private fun String.toSessionDetailGameLabel(): String = when (this) {
    "ac" -> "AC"
    "ace" -> "AC Evo"
    "acc" -> "ACC"
    "lmu" -> "LMU"
    else -> uppercase(Locale.US)
}
