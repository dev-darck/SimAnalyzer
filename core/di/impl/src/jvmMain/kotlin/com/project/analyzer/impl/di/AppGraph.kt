package com.project.analyzer.impl.di

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.hud.api.HudScope
import com.project.analyzer.navigation.api.NavigationScope
import com.project.analyzer.preference.api.PreferenceGraph
import com.project.analyzer.telemetry.analysis.api.service.RecordedTelemetryAnalysisService
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionStorage
import com.project.analyzer.utils.AppDirectoriesImpl
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes

@DependencyGraph(
    AppScope::class,
    additionalScopes = [SessionScope::class, ScreenScope::class, HudScope::class, NavigationScope::class],
)
interface AppGraph :
    AppComponent,
    PreferenceGraph {

    public val recordedTelemetryAnalysisService: RecordedTelemetryAnalysisService
    public val recordedTelemetrySessionStorage: RecordedTelemetrySessionStorage

    @DependencyGraph.Factory
    public fun interface Factory {

        public fun create(@Includes deps: Dependencies): AppGraph
    }

    public interface Dependencies {

        public val appDirectories: AppDirectoriesImpl
    }
}
