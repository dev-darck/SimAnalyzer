package com.project.analyzer.navigation.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
public sealed class Route(
    public val isRoot: Boolean,
    public val topLevel: Root,
) : NavKey {

    @Serializable
    public sealed class LiveRoot(public val root: Boolean = false) : Route(root, Root.Live) {

        @Serializable
        public object Live : LiveRoot(true)

        @Serializable
        public object LiveDetails : LiveRoot(false)
    }

    @Serializable
    public sealed class SessionRoot(public val root: Boolean = false) : Route(root, Root.Session) {

        @Serializable
        public data object Session : SessionRoot(true)

        @Serializable
        public data object SessionDetails : SessionRoot(false)
    }

    @Serializable
    public sealed class SetupRoot(public val root: Boolean = false) : Route(root, Root.Setup) {

        @Serializable
        public data object Setup : SetupRoot(true)
    }

    @Serializable
    public sealed class SettingsRoot(public val root: Boolean = false) : Route(root, Root.Settings) {

        @Serializable
        public data object Settings : SettingsRoot(true)

        @Serializable
        public data object HudSettings : SettingsRoot(false)
    }
}
