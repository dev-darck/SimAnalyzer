package com.project.analyzer.navigation.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
public sealed class Route(
    public val isRoot: Boolean
) : NavKey {

    @Serializable
    public sealed class HomeRoot(
        public val root: Boolean = false
    ) : Route(root) {

        @Serializable
        public data object Home : HomeRoot(true)
    }

    @Serializable
    public sealed class TelemetryRoot(
        public val root: Boolean = false
    ) : Route(root) {

        @Serializable
        public data object TelemetryDetails : TelemetryRoot(true)
    }

    @Serializable
    public sealed class SettingsRoot(
        public val root: Boolean = false
    ) : Route(root) {

        @Serializable
        public data object Settings : SettingsRoot(true)
    }
}
