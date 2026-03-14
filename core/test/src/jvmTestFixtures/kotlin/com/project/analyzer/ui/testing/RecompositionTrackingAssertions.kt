package com.project.analyzer.ui.testing

import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import com.project.analyzer.ui.modifier.RecompositionCountKey
import com.project.analyzer.ui.modifier.UiTestSemanticsConstants

fun SemanticsNodeInteraction.fetchRecompositionCount(): Int {
    val provider = fetchSemanticsNode(
        errorMessageOnFail = "Expected node to exist before reading recomposition count.",
    ).config.getOrNull(RecompositionCountKey)
        ?: error(
            "${UiTestSemanticsConstants.RecompositionCountName} semantics is not available for this node. " +
                "Attach Modifier.trackRecompositions() to the composable under test.",
        )

    return provider()
}

fun SemanticsNodeInteraction.assertRecompositionCountAtMost(maxInclusive: Int): SemanticsNodeInteraction =
    assertRecompositionCountIn(minInclusive = 0, maxInclusive = maxInclusive)

fun SemanticsNodeInteraction.assertRecompositionCountAtLeast(minInclusive: Int): SemanticsNodeInteraction =
    assertRecompositionCountIn(minInclusive = minInclusive, maxInclusive = Int.MAX_VALUE)

fun SemanticsNodeInteraction.assertRecompositionCountIn(
    minInclusive: Int,
    maxInclusive: Int,
): SemanticsNodeInteraction {
    require(minInclusive <= maxInclusive) {
        "Expected minInclusive <= maxInclusive, but was $minInclusive > $maxInclusive."
    }

    val actual = fetchRecompositionCount()
    if (actual !in minInclusive..maxInclusive) {
        throw AssertionError(
            "Expected recomposition count in [$minInclusive, $maxInclusive], but was $actual.",
        )
    }

    return this
}
