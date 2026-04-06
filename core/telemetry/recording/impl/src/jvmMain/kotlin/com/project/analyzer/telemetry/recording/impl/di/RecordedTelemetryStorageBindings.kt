package com.project.analyzer.telemetry.recording.impl.di

import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryPayloadDecoderRegistry
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionReader
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionStorage
import com.project.analyzer.telemetry.recording.impl.reader.RecordedTelemetrySessionReaderImpl
import com.project.analyzer.telemetry.recording.impl.reader.registry.RecordedTelemetryPayloadDecoderRegistryImpl
import com.project.analyzer.telemetry.recording.impl.reader.storage.RecordedTelemetrySessionStorageImpl
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
interface RecordedTelemetryStorageBindings {

    companion object {

        @Provides
        private fun provideRecordedTelemetrySessionStorage(
            impl: RecordedTelemetrySessionStorageImpl,
        ): RecordedTelemetrySessionStorage = impl

        @Provides
        private fun provideRecordedTelemetryPayloadDecoderRegistry(
            impl: RecordedTelemetryPayloadDecoderRegistryImpl,
        ): RecordedTelemetryPayloadDecoderRegistry = impl

        @Provides
        private fun provideRecordedTelemetrySessionReader(
            impl: RecordedTelemetrySessionReaderImpl,
        ): RecordedTelemetrySessionReader = impl
    }
}
