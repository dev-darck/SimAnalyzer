package com.analyzer.settings.data.lmu

import com.analyzer.settings.domain.model.LMU_PLUGIN_REPOSITORY_URL

internal const val LMU_PLUGIN_CACHE_DIR_NAME: String = "lmu-plugin"
internal const val LMU_PLUGIN_DIR_NAME: String = "Plugins"
internal const val LMU_USER_DATA_DIR_NAME: String = "UserData"
internal const val LMU_PLAYER_DIR_NAME: String = "player"
internal const val LMU_CUSTOM_PLUGIN_VARIABLES_FILE_NAME: String = "CustomPluginVariables.JSON"
internal const val LMU_PLUGIN_DLL_NAME: String = "rFactor2SharedMemoryMapPlugin64.dll"
internal const val LMU_PLUGIN_USER_AGENT: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) SimAnalyzer/1.0"
internal const val LMU_PLUGIN_ENABLED_KEY: String = " Enabled"

internal val LMU_PLUGIN_README_URLS: List<String> = listOf(
    "https://raw.githubusercontent.com/TheIronWolfModding/rF2SharedMemoryMapPlugin/master/README.md",
    "https://raw.githubusercontent.com/TheIronWolfModding/rF2SharedMemoryMapPlugin/main/README.md",
    "$LMU_PLUGIN_REPOSITORY_URL/raw/master/README.md",
    "$LMU_PLUGIN_REPOSITORY_URL/raw/main/README.md",
)

internal val LMU_PLUGIN_REQUIRED_SETTINGS: Map<String, Int> = linkedMapOf(
    LMU_PLUGIN_ENABLED_KEY to 1,
    "DebugISIInternals" to 0,
    "DebugOutputLevel" to 0,
    "DebugOutputSource" to 1,
    "DedicatedServerMapGlobally" to 0,
    "EnableDirectMemoryAccess" to 0,
    "EnableHWControlInput" to 1,
    "EnableRulesControlInput" to 0,
    "EnableWeatherControlInput" to 0,
    "UnsubscribedBuffersMask" to 160,
)
