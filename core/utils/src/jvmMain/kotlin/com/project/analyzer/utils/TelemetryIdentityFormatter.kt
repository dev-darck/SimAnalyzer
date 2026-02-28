package com.project.analyzer.utils

public object TelemetryIdentityFormatter {

    private val knownUppercaseTokens = setOf(
        "ac",
        "acc",
        "abs",
        "amg",
        "bmw",
        "drs",
        "ers",
        "gp",
        "gt",
        "gt2",
        "gt3",
        "gt4",
        "gte",
        "hud",
        "id",
        "kmh",
        "lmp1",
        "lmp2",
        "lmu",
        "mp4",
        "pit",
        "rpm",
        "rsr",
        "tc",
        "tcr",
        "ui",
    )

    public fun formatTrackName(trackName: String?, trackId: String? = null, layoutId: String? = null): String? {
        val rawName = trackName.normalizeOrNull()
        val normalizedTrackId = trackId.normalizeOrNull()
        val normalizedLayoutId = layoutId.normalizeOrNull()

        if (rawName != null && !looksLikeSlug(rawName)) {
            val layoutLabel = normalizedLayoutId?.let(::humanizeCompoundId)
            return appendLayoutIfNeeded(
                base = rawName.toReadablePhrase(),
                layout = layoutLabel,
            )
        }

        if (normalizedTrackId != null) {
            return humanizeCompoundId(normalizedTrackId)
        }
        if (rawName != null) {
            return humanizeCompoundId(rawName)
        }
        return normalizedLayoutId?.let(::humanizeCompoundId)
    }

    public fun formatCarName(carName: String? = null, carModel: String? = null): String? {
        val rawName = carName.normalizeOrNull()
        if (rawName != null && !looksLikeSlug(rawName)) {
            return rawName.toReadablePhrase()
        }

        val normalizedCarModel = carModel.normalizeOrNull() ?: rawName ?: return null
        return humanizeCompoundId(stripKnownCarPrefixes(normalizedCarModel))
    }

    public fun humanizeId(value: String?): String? = value.normalizeOrNull()?.let(::humanizeCompoundId)

    private fun appendLayoutIfNeeded(base: String, layout: String?): String {
        if (layout == null) return base
        val normalizedBase = normalizeForCompare(base)
        val normalizedLayout = normalizeForCompare(layout)
        if (normalizedLayout.isBlank()) return base
        return if (normalizedBase.endsWith(normalizedLayout) || normalizedBase.contains(" $normalizedLayout")) {
            base
        } else {
            "$base $layout"
        }
    }

    private fun stripKnownCarPrefixes(value: String): String {
        val lowered = value.lowercase()
        return when {
            lowered.startsWith("ks_") -> value.drop(3)
            lowered.startsWith("ac_") -> value.drop(3)
            lowered.startsWith("acc_") -> value.drop(4)
            else -> value
        }
    }

    private fun humanizeCompoundId(value: String): String = value
        .replace('\\', '_')
        .replace('/', '_')
        .replace('-', '_')
        .split('_')
        .filter { it.isNotBlank() }
        .joinToString(" ") { token -> token.toDisplayToken() }
        .normalizeWhitespace()

    private fun String.toDisplayToken(): String {
        val normalized = lowercase()
        if (normalized in knownUppercaseTokens) return normalized.uppercase()
        return replaceFirstChar { char ->
            if (char.isLowerCase()) {
                char.titlecase()
            } else {
                char.toString()
            }
        }
    }

    private fun looksLikeSlug(value: String): Boolean {
        if ('_' in value || '/' in value || '\\' in value || '-' in value) return true
        if (' ' in value) return false
        return value == value.lowercase()
    }

    private fun String.normalizeWhitespace(): String = trim().replace(Regex("\\s+"), " ")

    private fun String.toReadablePhrase(): String {
        val normalized = normalizeWhitespace()
        return if (normalized == normalized.lowercase()) {
            humanizeCompoundId(normalized)
        } else {
            normalized
        }
    }

    private fun normalizeForCompare(value: String): String = value
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    private fun String?.normalizeOrNull(): String? = this?.trim()?.takeIf { it.isNotBlank() }
}
