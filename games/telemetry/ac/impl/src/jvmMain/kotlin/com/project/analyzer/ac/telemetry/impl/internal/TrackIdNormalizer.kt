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

        val base = normalizeToken(track)
        if (base.isBlank()) {
            updateCache(track, layoutKey, "")
            return ""
        }

        val layoutToken = layoutKey?.let(::normalizeToken).orEmpty()
        val result = if (layoutToken.isBlank()) {
            base
        } else if (base.endsWith("_$layoutToken")) {
            base
        } else {
            "${base}_$layoutToken"
        }

        updateCache(track, layoutKey, result)
        return result
    }

    private fun updateCache(track: String, layout: String?, result: String) {
        lastTrack = track
        lastLayout = layout
        lastResult = result
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
}
