package com.project.analyzer.impl.di

import com.project.analyzer.telemetry.analysis.api.service.RecordedTelemetryAnalysisService
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionDefaults
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionBundle
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionStorage
import com.project.analyzer.utils.resolveAppDirectories
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.runBlocking
import org.junit.Test

class SampleTelemetryAnalysisInspectionTest {

    @Test
    fun inspectSampleTelemetrySessions() = runBlocking {
        val telemetryRoot = resolveTelemetryRoot()
        if (telemetryRoot.isBlank()) {
            println(
                "Skip sample telemetry inspection: no '$SAMPLE_TELEMETRY_DIR_PROP' system property " +
                    "or '$SAMPLE_TELEMETRY_DIR_ENV' environment variable.",
            )
            return@runBlocking
        }

        val appDirectories = resolveAppDirectories()
        val graph = createGraphFactory<AppGraph.Factory>().create(
            object : AppGraph.Dependencies {
                override val appDirectories = appDirectories
            },
        )

        val previousTelemetryRoot = graph.preference.getOrNull(TelemetryAcquisitionDefaults.KEY_STORAGE_LOCATION)
        graph.preference.put(TelemetryAcquisitionDefaults.KEY_STORAGE_LOCATION to telemetryRoot)

        try {
            inspectBundles(
                storage = graph.recordedTelemetrySessionStorage,
                analysisService = graph.recordedTelemetryAnalysisService,
            )
        } finally {
            if (previousTelemetryRoot != null) {
                graph.preference.put(TelemetryAcquisitionDefaults.KEY_STORAGE_LOCATION to previousTelemetryRoot)
            }
        }
    }

    private suspend fun inspectBundles(
        storage: RecordedTelemetrySessionStorage,
        analysisService: RecordedTelemetryAnalysisService,
    ) {
        val bundles = storage.loadBundles(forceRefresh = true)
            .sortedByDescending { bundle -> bundle.metadata.startedAtMs }

        println("=== Sample telemetry bundle groups: ${bundles.size} ===")
        bundles.take(MAX_BUNDLES_TO_INSPECT).forEach { bundle ->
            inspectBundle(bundle, analysisService)
        }
    }

    private suspend fun inspectBundle(
        bundle: RecordedTelemetrySessionBundle,
        analysisService: RecordedTelemetryAnalysisService,
    ) {
        val metadata = bundle.metadata
        println("")
        println("=== Session ${bundle.sessionId} | ${metadata.trackName ?: metadata.trackId} | ${metadata.carName ?: metadata.carModel} ===")
        println("frames=${metadata.frameCount}, rate=${metadata.samplingRateHz}Hz, startedAt=${metadata.startedAtMs}")

        val shellReport = analysisService.loadSessionReportShell(
            sessionId = bundle.sessionId,
            includeTrackMap = true,
            forceRefresh = true,
        )
        if (shellReport == null) {
            println("shellReport=null")
            return
        }

        val report = analysisService.enrichSessionReport(shellReport)
        val analysis = report.comprehensiveAnalysis

        println("highlights=${report.highlights.size}, laps=${report.laps.size}, samples=${report.samples.size}")
        println("narrative=${analysis?.narrative.orEmpty()}")

        println("-- top highlights --")
        report.highlights
            .sortedByDescending { highlight -> highlight.priority }
            .take(MAX_HIGHLIGHTS_TO_PRINT)
            .forEach { highlight ->
                println(
                    "highlight category=${highlight.category} source=${highlight.diagnosisSource} lap=${highlight.lapNumber} " +
                        "corner=${highlight.cornerNumber} delta=${highlight.deltaMs} title=${highlight.title} " +
                        "description=${highlight.description} recommendation=${highlight.recommendation}",
                )
            }

        println("-- setup recommendations --")
        analysis?.setupRecommendations
            .orEmpty()
            .take(MAX_SETUP_TO_PRINT)
            .forEach { recommendation ->
                println(
                    "setup category=${recommendation.category} change=${recommendation.suggestedChange} " +
                        "reason=${recommendation.reason} benefit=${recommendation.expectedBenefit} " +
                        "confidence=${recommendation.confidence} corners=${recommendation.affectedCorners}",
                )
            }

        println("-- corner analyses --")
        analysis?.cornerAnalyses
            .orEmpty()
            .sortedBy { corner -> corner.cornerScore }
            .take(MAX_CORNERS_TO_PRINT)
            .forEach { corner ->
                println(
                    "corner number=${corner.cornerNumber} score=${corner.cornerScore} timeDelta=${corner.timeDelta} " +
                        "issue=${corner.primaryIssue} recommendation=${corner.recommendation} setup=${corner.setupSuggestion} " +
                        "entry=${corner.entrySpeed}/${corner.referenceEntrySpeed} apex=${corner.apexSpeed}/${corner.referenceApexSpeed} " +
                        "exit=${corner.exitSpeed}/${corner.referenceExitSpeed}",
                )
            }
    }

    private companion object {

        const val SAMPLE_TELEMETRY_DIR_PROP = "sampleTelemetryDir"
        const val SAMPLE_TELEMETRY_DIR_ENV = "SAMPLE_TELEMETRY_DIR"
        const val MAX_BUNDLES_TO_INSPECT = 4
        const val MAX_HIGHLIGHTS_TO_PRINT = 8
        const val MAX_SETUP_TO_PRINT = 6
        const val MAX_CORNERS_TO_PRINT = 8
    }

    private fun resolveTelemetryRoot(): String {
        val propertyValue = System.getProperty(SAMPLE_TELEMETRY_DIR_PROP).orEmpty().trim()
        if (propertyValue.isNotBlank()) {
            return propertyValue
        }
        return System.getenv(SAMPLE_TELEMETRY_DIR_ENV).orEmpty().trim()
    }
}
