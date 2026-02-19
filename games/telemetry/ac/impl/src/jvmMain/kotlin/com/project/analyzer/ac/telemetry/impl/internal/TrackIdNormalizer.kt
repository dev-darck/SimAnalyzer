package com.project.analyzer.ac.telemetry.impl.internal

internal object TrackIdNormalizer {

    @Volatile
    private var lastTrack: String = ""

    @Volatile
    private var lastLayout: String? = null

    @Volatile
    private var lastResult: String = ""

    fun normalize(track: String, layout: String? = null): String {
        val layoutKey = layout?.takeIf { it.isNotBlank() }
        if (track == lastTrack && layoutKey == lastLayout) return lastResult

        val base = normalizeBaseToken(track)
        if (base.isBlank()) {
            updateCache(track, layoutKey, "")
            return ""
        }

        val normalizedLayout = layoutKey?.let(::normalizeLayoutToken).orEmpty()
        val result = buildTrackId(base, normalizedLayout)

        updateCache(track, layoutKey, result)
        return result
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

        return if (start >= end) "" else if (start == 0 && end == sb.length) sb.toString() else sb.substring(start, end)
    }

    private val LAYOUT_ALIASES: Map<String, String> = mapOf(
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
    )

    private val NON_DRIVABLE_TRACK_TOKENS: Set<String> = setOf(
        "interns",
        "car_dealership",
        "showroom",
        "main_menu"
    )

    private val TRACK_FOLDER_IN_PATH = Regex(
        pattern = """content[\\/]+tracks[\\/]+([^\\/]+)""",
        option = RegexOption.IGNORE_CASE
    )

    private val TRAILING_YEAR_SUFFIX = Regex("_(19|20)\\d{2}$")
}
