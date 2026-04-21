package com.analyzer.settings.api

public enum class AppCloseBehavior {
    AskEveryTime,
    Exit,
    MinimizeToTray,
    ;

    public companion object {

        public fun fromPreference(value: String?): AppCloseBehavior =
            entries.firstOrNull { it.name == value } ?: AskEveryTime
    }
}
