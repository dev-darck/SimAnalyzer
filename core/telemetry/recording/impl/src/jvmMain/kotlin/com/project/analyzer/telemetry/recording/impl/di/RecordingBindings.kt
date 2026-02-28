package com.project.analyzer.telemetry.recording.impl.di

import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.recording.api.recorder.TelemetryRecorder
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingController
import com.project.analyzer.telemetry.recording.impl.controller.TelemetryRecordingControllerImpl
import com.project.analyzer.telemetry.recording.impl.file.FileTelemetryRecorder
import com.project.analyzer.telemetry.recording.impl.file.TelemetrySessionCompressionService
import com.project.analyzer.telemetry.recording.impl.file.TelemetrySessionCompressor
import com.project.analyzer.telemetry.recording.impl.file.codec.DefaultFrameStorageCodecFactory
import com.project.analyzer.telemetry.recording.impl.file.codec.FrameStorageCodecFactory
import com.project.analyzer.telemetry.recording.impl.file.engine.DefaultTelemetryFileActiveSessionsFactory
import com.project.analyzer.telemetry.recording.impl.file.engine.DefaultTelemetryFileCommandProcessorFactory
import com.project.analyzer.telemetry.recording.impl.file.engine.TelemetryFileActiveSessionsFactory
import com.project.analyzer.telemetry.recording.impl.file.engine.TelemetryFileCommandProcessorFactory
import com.project.analyzer.telemetry.recording.impl.file.pipeline.FileTelemetryFrameWriteAdapter
import com.project.analyzer.telemetry.recording.impl.file.session.FileTelemetrySessionStore
import com.project.analyzer.telemetry.recording.impl.file.session.TelemetryFileSessionStore
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(SessionScope::class)
@BindingContainer
interface RecordingBindings {
    companion object {

        @Provides
        private fun provideRecordingController(impl: TelemetryRecordingControllerImpl): TelemetryRecordingController =
            impl

        @Provides
        private fun provideTelemetryRecorder(impl: FileTelemetryRecorder): TelemetryRecorder = impl

        @Provides
        private fun provideFrameStorageCodecFactory(): FrameStorageCodecFactory = DefaultFrameStorageCodecFactory

        @Provides
        private fun provideFrameWriteAdapters(): List<FileTelemetryFrameWriteAdapter> = emptyList()

        @Provides
        private fun provideFileSessionStore(impl: FileTelemetrySessionStore): TelemetryFileSessionStore = impl

        @Provides
        private fun provideActiveSessionsFactory(): TelemetryFileActiveSessionsFactory =
            DefaultTelemetryFileActiveSessionsFactory

        @Provides
        private fun provideCommandProcessorFactory(
            sessionStore: TelemetryFileSessionStore,
            activeSessionsFactory: TelemetryFileActiveSessionsFactory,
        ): TelemetryFileCommandProcessorFactory = DefaultTelemetryFileCommandProcessorFactory(
            sessionStore = sessionStore,
            activeSessionsFactory = activeSessionsFactory,
        )

        @Provides
        private fun provideCompressionService(impl: TelemetrySessionCompressor): TelemetrySessionCompressionService =
            impl
    }
}
