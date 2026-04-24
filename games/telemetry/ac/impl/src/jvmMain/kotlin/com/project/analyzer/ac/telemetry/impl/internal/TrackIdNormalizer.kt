package com.project.analyzer.ac.telemetry.impl.internal

internal object TrackIdNormalizer {

    @Volatile
    private var lastTrack: String = ""

    @Volatile
    private var lastLayout: String? = null

    @Volatile
    private var lastResult: String = ""

    fun normalize(track: String, layout: String? = null): String {
        val layoutKey = normalizeLayoutId(layout)
        if (track == lastTrack && layoutKey == lastLayout) return lastResult

        val rawBase = normalizeBaseToken(track)
        val normalizedIdentity = splitTrackAndLayout(
            trackToken = rawBase,
            explicitLayout = layoutKey,
        )
        val base = canonicalizeBaseToken(
            base = normalizedIdentity.trackToken,
            layout = normalizedIdentity.layoutToken,
        )
        if (base.isBlank()) {
            updateCache(track, layoutKey, "")
            return ""
        }

        val normalizedLayout = normalizedIdentity.layoutToken?.let(::normalizeLayoutToken).orEmpty()
        val result = buildTrackId(base, normalizedLayout)

        updateCache(track, layoutKey, result)
        return result
    }

    fun normalizeLayoutId(raw: String?): String? {
        val layoutKey = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return normalizeLayoutToken(layoutKey).takeIf { it.isNotBlank() }
    }

    private fun updateCache(track: String, layout: String?, result: String) {
        lastTrack = track
        lastLayout = layout
        lastResult = result
    }

    private fun normalizeBaseToken(raw: String): String {
        val fromPath = extractTrackFolderFromPath(raw)
        var token = normalizeToken(fromPath ?: raw)
        if (token.isBlank()) return ""

        token = token
            .removePrefix("ks_")
            .removePrefix("acc_")
            .removePrefix("ac_")
            .removeSuffix("_ev")
            .replace(TRAILING_YEAR_SUFFIX, "")
            .trim('_')

        if (token.isBlank()) return ""
        if (token in NON_DRIVABLE_TRACK_TOKENS) return ""
        return token
    }

    private fun splitTrackAndLayout(trackToken: String, explicitLayout: String?): NormalizedTrackIdentity {
        if (trackToken.isBlank()) {
            return NormalizedTrackIdentity(trackToken = "", layoutToken = explicitLayout)
        }
        val resolvedLayout = explicitLayout ?: inferEmbeddedLayout(trackToken)
        if (resolvedLayout == null) {
            return NormalizedTrackIdentity(trackToken = trackToken, layoutToken = null)
        }
        val strippedTrack = stripEmbeddedLayoutSuffix(
            trackToken = trackToken,
            layout = resolvedLayout,
        )
        return NormalizedTrackIdentity(
            trackToken = strippedTrack.ifBlank { trackToken },
            layoutToken = resolvedLayout,
        )
    }

    private fun inferEmbeddedLayout(trackToken: String): String? =
        KNOWN_LAYOUT_SUFFIXES.firstNotNullOfOrNull { suffix ->
            if (!trackToken.endsWith("_$suffix")) return@firstNotNullOfOrNull null
            val strippedTrack = trackToken.removeSuffix("_$suffix").trimEnd('_')
            if (strippedTrack.isBlank()) return@firstNotNullOfOrNull null
            normalizeLayoutToken(suffix).takeIf { it.isNotBlank() }
        }

    private fun stripEmbeddedLayoutSuffix(trackToken: String, layout: String): String {
        val suffixes = layoutSuffixVariants(layout)
        suffixes.forEach { suffix ->
            if (trackToken.endsWith("_$suffix")) {
                val strippedTrack = trackToken.removeSuffix("_$suffix").trimEnd('_')
                if (strippedTrack.isNotBlank()) return strippedTrack
            }
        }
        return trackToken
    }

    private fun canonicalizeBaseToken(base: String, layout: String?): String {
        var normalized = base
        val layoutKey = layout.orEmpty()
        if (layoutKey.isNotBlank() && normalized.endsWith("_$layoutKey")) {
            normalized = normalized.removeSuffix("_$layoutKey")
        }
        normalized = normalized.removeSuffix("_layout").trim('_')
        if (normalized.isBlank()) return ""
        return BASE_ALIASES[normalized] ?: normalized
    }

    private fun normalizeLayoutToken(raw: String): String {
        val fromPath = extractPathLeaf(raw)
        var normalized = normalizeToken(fromPath ?: raw)
        if (normalized.startsWith("layout_")) {
            normalized = normalized.removePrefix("layout_")
        }
        normalized = normalized
            .replace(TRAILING_YEAR_SUFFIX, "")
            .trim('_')
        if (normalized.isBlank()) return ""
        return LAYOUT_ALIASES[normalized] ?: normalized
    }

    private fun buildTrackId(base: String, layout: String): String {
        if (layout.isBlank()) return base
        if (layout == base) return base
        if (layout.startsWith("${base}_")) return layout
        return if (base.endsWith("_$layout")) base else "${base}_$layout"
    }

    private fun extractTrackFolderFromPath(raw: String): String? {
        val m = TRACK_FOLDER_IN_PATH.find(raw) ?: return null
        return m.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private fun extractPathLeaf(raw: String): String? {
        if (!raw.contains('/') && !raw.contains('\\')) return null
        val segment = raw.substringAfterLast('/').substringAfterLast('\\')
        val withoutExt = segment.substringBefore('.')
        return withoutExt.takeIf { it.isNotBlank() }
    }

    private fun normalizeToken(raw: String): String {
        if (raw.isBlank()) return ""

        val sb = StringBuilder(raw.length)
        var prevUnderscore = false
        for (ch in raw) {
            val c = ch.lowercaseChar()
            val isAlphaNum = (c in 'a'..'z') || (c in '0'..'9')
            if (isAlphaNum) {
                sb.append(c)
                prevUnderscore = false
            } else if (!prevUnderscore) {
                sb.append('_')
                prevUnderscore = true
            }
        }

        var start = 0
        var end = sb.length
        while (start < end && sb[start] == '_') start++
        while (end > start && sb[end - 1] == '_') end--

        return if (start >= end) {
            ""
        } else if (start == 0 && end == sb.length) {
            sb.toString()
        } else {
            sb.substring(start, end)
        }
    }

    private val LAYOUT_ALIASES: Map<String, String> = mapOf(
        "grand_prix" to "gp",
        "gp_circuit" to "gp",
        "gp_circuit_shortcut" to "gp_short",
        "gp_circuit_short" to "gp_short",
        "gp_shortcut" to "gp_short",
        "short" to "gp_short",
        "shortcut" to "gp_short",
        "full_course" to "gp",
        "full" to "gp",
        "national_circuit" to "national",
        "international_circuit" to "international",
        "gp_strecke" to "gp_strecke",
        "strecke" to "gp_strecke",
        "24_hr" to "24h",
        "24_hour" to "24h",
        "24hours" to "24h",
        "24_hour_layout" to "24h",
        "track_layout" to "gp",
        "imola" to "gp",
        "laguna_seca" to "gp",
    )

    private val BASE_ALIASES: Map<String, String> = mapOf(
        "circuit_de_spa_francorchamps" to "spa",
        "spa_francorchamps" to "spa",
        "cota" to "circuit_of_the_americas",
        "donington" to "donington_park",
        "fuji_speedway" to "fuji",
        "red_bull_ring" to "redbull_ring",
        "sebring_international_raceway" to "sebring",
        "watkins_glen_international" to "watkins_glen",
        "paul_ricard_layout" to "paul_ricard",
    )

    private val TRACK_FOLDER_IN_PATH = Regex(
        pattern = """content[\\/]+tracks[\\/]+([^\\/]+)""",
        option = RegexOption.IGNORE_CASE,
    )

    private val TRAILING_YEAR_SUFFIX = Regex("_(19|20)\\d{2}$")

    private val LAYOUT_VARIANTS_BY_CANONICAL: Map<String, List<String>> = buildMap {
        val variants = linkedMapOf<String, LinkedHashSet<String>>()
        (LAYOUT_ALIASES.keys + LAYOUT_ALIASES.values).forEach { rawVariant ->
            val normalizedVariant = normalizeLayoutVariant(rawVariant)
            if (normalizedVariant.isBlank()) return@forEach
            val canonicalVariant = LAYOUT_ALIASES[normalizedVariant] ?: normalizedVariant
            variants.getOrPut(canonicalVariant) { linkedSetOf() }.add(normalizedVariant)
        }
        variants.forEach { (canonical, canonicalVariants) ->
            put(canonical, canonicalVariants.sortedByDescending(String::length))
        }
    }

    private val KNOWN_LAYOUT_SUFFIXES: List<String> = LAYOUT_VARIANTS_BY_CANONICAL.values
        .flatten()
        .distinct()
        .sortedByDescending(String::length)

    private val NON_DRIVABLE_TRACK_TOKENS: Set<String> = setOf(
        "interns",
        "car_dealership",
        "showroom",
        "main_menu",
    )

    private data class NormalizedTrackIdentity(val trackToken: String, val layoutToken: String?)

    private fun layoutSuffixVariants(layout: String): List<String> =
        LAYOUT_VARIANTS_BY_CANONICAL[layout].orEmpty().ifEmpty { listOf(layout) }

    private fun normalizeLayoutVariant(raw: String): String {
        val fromPath = extractPathLeaf(raw)
        var normalized = normalizeToken(fromPath ?: raw)
        if (normalized.startsWith("layout_")) {
            normalized = normalized.removePrefix("layout_")
        }
        return normalized
            .replace(TRAILING_YEAR_SUFFIX, "")
            .trim('_')
    }
}
