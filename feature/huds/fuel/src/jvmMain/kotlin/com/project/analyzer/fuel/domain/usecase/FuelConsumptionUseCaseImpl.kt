package com.project.analyzer.fuel.domain.usecase

import com.project.analyzer.api.di.IO
import com.project.analyzer.fuel.data.model.SavedFuelData
import com.project.analyzer.fuel.domain.model.FuelPhase
import com.project.analyzer.fuel.domain.model.FuelResult
import com.project.analyzer.fuel.domain.model.TelemetryEvent
import com.project.analyzer.fuel.domain.model.toTelemetryEvent
import com.project.analyzer.fuel.domain.predictor.FuelConsumptionEngine
import com.project.analyzer.fuel.domain.repository.FuelRepository
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.ac.api.model.TelemetryFrame
import com.project.analyzer.utils.logger
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

@Inject
internal class FuelConsumptionUseCaseImpl(
    telemetry: TelemetryLifecycle,
    @IO
    dispatcher: CoroutineDispatcher,
    private val engine: FuelConsumptionEngine,
    private val repository: FuelRepository,
) : FuelConsumptionUseCase {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private var currentCar: String = ""
    private var currentTrack: String = ""
    private var sessionPeakLitersPerLap: Double = 0.0
    private var sessionBestValidLapTimeMs: Int? = null
    private var savedFuelData: SavedFuelData? = null

    private val manualResetFlow = MutableSharedFlow<FuelResult>(extraBufferCapacity = 1)

    override val fuelEstimates: Flow<FuelResult> = merge(
        combine(
            telemetry.frames,
            telemetry.events
                .mapNotNull { it.toTelemetryEvent() }
                .distinctUntilChanged(),
            ::Pair
        ).map { (frame, event) ->
            processFrame(event, frame)
        },
        manualResetFlow
    ).flowOn(dispatcher)

    override suspend fun resetAll() {
        engine.reset()
        sessionPeakLitersPerLap = 0.0
        sessionBestValidLapTimeMs = null
        savedFuelData = null

        if (currentCar.isNotBlank() && currentTrack.isNotBlank()) {
            repository.clear(currentCar, currentTrack)
        }

        manualResetFlow.emit(FuelResult.Reset)
    }

    private suspend fun processFrame(event: TelemetryEvent, frame: TelemetryFrame): FuelResult {
        if (event == TelemetryEvent.SessionEnded) {
            saveDataIfNeeded()
            return FuelResult.SessionEnded
        }

        val car = frame.session?.car?.carModel.orEmpty()
        val track = frame.session?.track?.trackId.orEmpty()

        if (car != currentCar || track != currentTrack) {
            logger.info { "FuelConsumptionUseCaseImpl changed car=$car track=$track" }
            saveDataIfNeeded()

            currentCar = car
            currentTrack = track
            sessionPeakLitersPerLap = 0.0
            sessionBestValidLapTimeMs = null
            savedFuelData = repository.load(currentCar, currentTrack)
            engine.reset()
            return FuelResult.Reset
        }

        if (event != TelemetryEvent.SessionStarted) {
            return FuelResult.NoData
        }

        val estimate = engine.onFrame(frame, savedFuelData) ?: return FuelResult.NoData

        if (estimate.phase == FuelPhase.PER_LAP && estimate.isCurrentLapValid) {
            estimate.litersPerLap?.let { lpl ->
                if (lpl > sessionPeakLitersPerLap) sessionPeakLitersPerLap = lpl
            }
        }

        frame.lap?.bestValidLapTimeMs?.let { bestValidMs ->
            if (bestValidMs > 0) {
                val current = sessionBestValidLapTimeMs
                if (current == null || bestValidMs < current) {
                    sessionBestValidLapTimeMs = bestValidMs
                }
            }
        }

        return FuelResult.Data(estimate)
    }

    private fun saveDataIfNeeded() {
        if (currentCar.isBlank() || currentTrack.isBlank()) return
        if (sessionPeakLitersPerLap <= 0 && sessionBestValidLapTimeMs == null) return

        scope.launch {
            repository.updateIfBetter(
                carModel = currentCar,
                trackId = currentTrack,
                peakLitersPerLap = sessionPeakLitersPerLap.takeIf { it > 0 },
                bestValidLapTimeMs = sessionBestValidLapTimeMs
            )
        }
    }
}
