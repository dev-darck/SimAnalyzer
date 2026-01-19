package com.project.analyzer.fuel.di

import androidx.lifecycle.ViewModel
import com.project.analyzer.api.di.IO
import com.project.analyzer.fuel.data.FuelRepositoryImpl
import com.project.analyzer.fuel.domain.predictor.FuelConsumptionEngine
import com.project.analyzer.fuel.domain.repository.FuelRepository
import com.project.analyzer.fuel.domain.usecase.FuelConsumptionUseCase
import com.project.analyzer.fuel.domain.usecase.FuelConsumptionUseCaseImpl
import com.project.analyzer.fuel.presentation.viewmodel.FuelHudViewModel
import com.project.analyzer.hud.api.HudScope
import com.project.analyzer.preference.api.Preference
import com.project.analyzer.preference.api.SessionPref
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycle
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json

@BindingContainer
@ContributesTo(HudScope::class)
object FuelHudBindings {

    @Provides
    @SingleIn(HudScope::class)
    private fun provideFuelConsumptionUseCase(
        telemetry: TelemetryLifecycle,
        @IO
        dispatcher: CoroutineDispatcher,
        engine: FuelConsumptionEngine,
        repository: FuelRepository,
    ): FuelConsumptionUseCase =
        FuelConsumptionUseCaseImpl(
            telemetry = telemetry,
            dispatcher = dispatcher,
            engine = engine,
            repository = repository
        )

    @Provides
    @SingleIn(HudScope::class)
    private fun provideFuelRepository(
        @SessionPref
        preference: Preference,
        son: Json
    ): FuelRepository =
        FuelRepositoryImpl(
            preference = preference,
            json = son
        )

    @Provides
    @IntoMap
    @SingleIn(HudScope::class)
    @ViewModelKey(FuelHudViewModel::class)
    private fun provideFuelHudViewModel(
        useCase: FuelConsumptionUseCase
    ): ViewModel = FuelHudViewModel(useCase)
}
