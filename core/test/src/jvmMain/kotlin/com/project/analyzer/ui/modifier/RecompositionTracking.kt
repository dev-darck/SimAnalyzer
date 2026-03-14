package com.project.analyzer.ui.modifier

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import com.project.analyzer.test.BuildConfig

public object UiTestSemanticsConstants {

    public const val RecompositionCountName: String = "RecompositionCount"
}

internal typealias RecompositionCountProvider = () -> Int

internal val RecompositionCountKey: SemanticsPropertyKey<RecompositionCountProvider> =
    SemanticsPropertyKey(name = UiTestSemanticsConstants.RecompositionCountName)

private class RecompositionCounter {

    var count: Int = 0
}

public fun Modifier.trackRecompositions(): Modifier =
    if (BuildConfig.IS_DEBUG) {
        composed {
            val counter = remember { RecompositionCounter() }

            SideEffect {
                counter.count += 1
            }

            semantics {
                this[RecompositionCountKey] = { counter.count }
            }
        }
    } else {
        this
    }
