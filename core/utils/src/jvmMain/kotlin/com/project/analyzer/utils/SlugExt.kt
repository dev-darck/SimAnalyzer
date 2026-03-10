package com.project.analyzer.utils

import java.util.Locale

public fun String.toSlugId(allowDash: Boolean = false): String = trim()
    .lowercase(Locale.US)
    .replace(WHITESPACE_REGEX, "_")
    .replace(if (allowDash) NON_SLUG_WITH_DASH_REGEX else NON_SLUG_REGEX, "_")
    .replace(MULTIPLE_UNDERSCORES_REGEX, "_")
    .trim('_')

private val WHITESPACE_REGEX = Regex("""\s+""")
private val NON_SLUG_REGEX = Regex("""[^a-z0-9_]+""")
private val NON_SLUG_WITH_DASH_REGEX = Regex("""[^a-z0-9_-]+""")
private val MULTIPLE_UNDERSCORES_REGEX = Regex("""_+""")
