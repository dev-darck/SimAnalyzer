package com.project.analyzer.navigation.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
public sealed class Route(public val isRoot: Boolean, public val topLevel: Root) : NavKey {

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
        public data class SessionDetails(public val sessionId: Long) : SessionRoot(false) {

            override fun equals(other: Any?): Boolean = other is SessionDetails

            override fun hashCode(): Int = SessionDetails::class.hashCode()
        }
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

        @Serializable
        public data object DevSettings : SettingsRoot(false)
    }
}
