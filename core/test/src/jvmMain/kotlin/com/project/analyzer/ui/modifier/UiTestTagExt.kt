package com.project.analyzer.ui.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

private val WhitespaceRegex = "\\s+".toRegex()

public fun uiTestTagOf(vararg parts: Any?): UiTestTag = UiTestTag(buildUiTestTag(*parts))

public fun buildUiTestTag(vararg parts: Any?): String {
    val segments = parts.asSequence()
        .flatMap { part -> normalizeUiTestTagPart(part).asSequence() }
        .toList()

    require(segments.isNotEmpty()) {
        "UI test tag requires at least one non-blank segment."
    }

    return segments.joinToString(separator = TestTags.SegmentSeparator)
}

public fun Modifier.uiTestTag(tag: UiTestTag): Modifier = testTag(tag.value)

public fun Modifier.uiTestTag(vararg parts: Any?): Modifier = testTag(buildUiTestTag(*parts))

private fun normalizeUiTestTagPart(part: Any?): List<String> = when (part) {
    null -> emptyList()
    is UiTestTag -> listOf(part.value)
    is Enum<*> -> listOf(part.name.lowercase())
    else -> part.toString()
        .split(TestTags.SegmentSeparator)
        .mapNotNull(::normalizeUiTestTagSegment)
}

private fun normalizeUiTestTagSegment(segment: String): String? = segment
    .trim()
    .replace(WhitespaceRegex, TestTags.WhitespaceReplacement)
    .lowercase()
    .takeIf(String::isNotEmpty)
