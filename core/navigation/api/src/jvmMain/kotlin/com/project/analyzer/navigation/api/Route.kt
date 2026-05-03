package com.project.analyzer.navigation.api

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class Route(public val isRoot: Boolean, public val topLevel: Root) {

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
        public data class SessionDetails(public val sessionId: Long) : SessionRoot(false)

        @Serializable
        public data class SessionAnalysis(
            public val sessionId: Long,
            public val segmentId: Long? = null,
            public val lapNumber: Int? = null,
            public val referenceSessionId: Long? = null,
            public val referenceSegmentId: Long? = null,
            public val referenceLapNumber: Int? = null,
        ) : SessionRoot(false)
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

        @Serializable
        public data object DevCalibration : SettingsRoot(false)

        @Serializable
        public data class DevCalibrationVerify(public val trackId: String) : SettingsRoot(false)

        @Serializable
        public data object DevTrackMap : SettingsRoot(false)

        @Serializable
        public data object DevTrackMapLibrary : SettingsRoot(false)

        @Serializable
        public data object DevTelemetry : SettingsRoot(false)

        @Serializable
        public data object DevHud : SettingsRoot(false)

        @Serializable
        public data class TrackMapCalibrationEditor(
            public val gameId: String,
            public val trackId: String,
            public val layoutId: String? = null,
        ) : SettingsRoot(false)
    }
}
