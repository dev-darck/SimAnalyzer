package com.project.analyzer.game.impl

import com.project.analyzer.game.api.GameConfig
import com.project.analyzer.game.api.GameWindowInfo

internal fun GameConfig.matches(info: GameWindowInfo): Boolean {
    val titleOk = titlePatterns.isEmpty() || titlePatterns.any {
        info.title.contains(it, ignoreCase = true)
    }
    val procOk = processNames.isEmpty() || processNames.any {
        info.processName.equals(it, ignoreCase = true)
    }
    val classOk = windowClassNames.isEmpty() || windowClassNames.any {
        info.className.equals(it, ignoreCase = true)
    }
    return titleOk && procOk && classOk
}

internal fun GameConfig.matchesProcessName(processName: String): Boolean =
    processNames.any { processName.equals(it, ignoreCase = true) }

internal fun GameConfig.matchesTitle(title: String): Boolean =
    titlePatterns.any { title.contains(it, ignoreCase = true) }

internal fun GameConfig.matchesClassName(className: String): Boolean =
    windowClassNames.any { className.equals(it, ignoreCase = true) }
