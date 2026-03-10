package com.project.analyzer.utils

public object TrackIdentityAliasMatcher {

    public fun areEquivalent(
        trackId: String?,
        layoutId: String?,
        otherTrackId: String?,
        otherLayoutId: String?,
    ): Boolean {
        val primaryKeys = buildAliasKeys(
            trackId = trackId,
            layoutId = layoutId,
            extraLayouts = listOf(otherLayoutId),
        )
        if (primaryKeys.isEmpty()) return false

        val otherKeys = buildAliasKeys(
            trackId = otherTrackId,
            layoutId = otherLayoutId,
            extraLayouts = listOf(layoutId),
        )
        if (otherKeys.isEmpty()) return false

        return primaryKeys.any(otherKeys::contains)
    }

    public fun buildAliasKeys(
        trackId: String?,
        layoutId: String?,
        extraLayouts: Iterable<String?> = emptyList(),
    ): Set<String> {
        val normalizedTrackId = normalizeTrackToken(trackId)
        if (normalizedTrackId.isEmpty()) return emptySet()

        val normalizedLayouts = linkedSetOf<String>()
        normalizeLayoutToken(layoutId)
            .takeIf { it.isNotEmpty() }
            ?.let(normalizedLayouts::add)
        extraLayouts.forEach { extraLayout ->
            normalizeLayoutToken(extraLayout)
                .takeIf { it.isNotEmpty() }
                ?.let(normalizedLayouts::add)
        }

        return buildSet {
            add(normalizedTrackId)
            add(compactToken(normalizedTrackId))

            normalizedLayouts.forEach { normalizedLayout ->
                val combined = combineTrackAndLayout(
                    trackId = normalizedTrackId,
                    layoutId = normalizedLayout,
                )
                add(combined)
                add(compactToken(combined))

                val relaxed = canonicalizeTrackForLayout(
                    trackId = normalizedTrackId,
                    layoutId = normalizedLayout,
                )
                if (!relaxed.isNullOrBlank()) {
                    add(relaxed)
                    add(compactToken(relaxed))
                }
            }
        }.filterTo(linkedSetOf(), String::isNotBlank)
    }

    private fun canonicalizeTrackForLayout(trackId: String, layoutId: String): String? {
        val trackTokens = trackId.split('_').filter(String::isNotBlank)
        val layoutTokens = layoutId.split('_').filter(String::isNotBlank)
        if (trackTokens.isEmpty() || layoutTokens.isEmpty()) return null

        val overlap = suffixPrefixOverlap(trackTokens, layoutTokens)
        if (overlap == 0) return null

        val baseTokens = trimTrailingFamilyQualifiers(trackTokens.dropLast(overlap))
        if (baseTokens.isEmpty()) return null

        val canonical = (baseTokens + layoutTokens).joinToString("_")
        return canonical.takeUnless { it == trackId }
    }

    private fun suffixPrefixOverlap(trackTokens: List<String>, layoutTokens: List<String>): Int {
        val maxOverlap = minOf(trackTokens.size, layoutTokens.size)
        for (overlap in maxOverlap downTo 1) {
            if (trackTokens.takeLast(overlap) == layoutTokens.take(overlap)) {
                return overlap
            }
        }
        return 0
    }

    private fun trimTrailingFamilyQualifiers(tokens: List<String>): List<String> {
        if (tokens.isEmpty()) return emptyList()
        val result = tokens.toMutableList()
        while (result.isNotEmpty() && result.last() in FAMILY_QUALIFIERS) {
            result.removeLast()
        }
        return result
    }

    private fun combineTrackAndLayout(trackId: String, layoutId: String): String = when {
        layoutId.isBlank() -> trackId
        trackId == layoutId -> trackId
        trackId.endsWith("_$layoutId") -> trackId
        else -> "${trackId}_$layoutId"
    }

    private fun normalizeTrackToken(value: String?): String = value
        .orEmpty()
        .toSlugId()

    private fun normalizeLayoutToken(value: String?): String = normalizeTrackToken(value)
        .removePrefix("layout_")

    private fun compactToken(value: String): String = value.replace("_", "")

    private val FAMILY_QUALIFIERS: Set<String> = setOf(
        "gp",
        "national",
        "international",
        "sprint",
        "short",
        "long",
        "full",
        "club",
    )
}
