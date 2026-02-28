package com.project.analyzer.ac.telemetry.impl.di

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.debug.AcCalibrationDebugGateDetectorAdapter
import com.project.analyzer.ac.telemetry.impl.fallback.debug.AcCalibrationDebugLapAnalyzerAdapter
import com.project.analyzer.ac.telemetry.impl.fallback.detector.GateCrossingDetector
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.ac.api.debug.AcCalibrationDebugGateDetector
import com.project.analyzer.telemetry.ac.api.debug.AcCalibrationDebugLapAnalyzer
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(ScreenScope::class)
@BindingContainer
interface AcCalibrationDebugBindings {
    companion object {

        @Provides
        private fun provideDebugLapAnalyzer(delegate: FallbackLapAnalyzer): AcCalibrationDebugLapAnalyzer =
            AcCalibrationDebugLapAnalyzerAdapter(delegate)

        @Provides
        private fun provideDebugGateDetector(delegate: GateCrossingDetector): AcCalibrationDebugGateDetector =
            AcCalibrationDebugGateDetectorAdapter(delegate)
    }
}
