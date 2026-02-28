package com.project.analyzer.ac.telemetry.impl.internal.pipeline

import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger

internal class AcPollSnapshotPipeline(adapters: List<AcPollSnapshotAdapter>) {

    private val logger = logger()
    private val orderedAdapters = orderAdapters(adapters)

    fun onStateChanged(newState: com.project.analyzer.ac.telemetry.impl.internal.GameConnectionState) {
        orderedAdapters.forEach { adapter ->
            try {
                adapter.onStateChanged(newState)
            } catch (t: Throwable) {
                logStageError(adapter, "onStateChanged", t)
                throw t
            }
        }
    }

    fun apply(context: AcPollSnapshotAdapterContext) {
        orderedAdapters.forEach { adapter ->
            try {
                adapter.apply(context)
            } catch (t: Throwable) {
                logStageError(adapter, "apply", t)
                throw t
            }
        }
    }

    fun onStop() {
        orderedAdapters.forEach { adapter ->
            try {
                adapter.onStop()
            } catch (t: Throwable) {
                logStageError(adapter, "onStop", t)
                throw t
            }
        }
    }

    private fun logStageError(adapter: AcPollSnapshotAdapter, op: String, error: Throwable) {
        logger.atError(RATE_LIMITED) {
            message = "AC snapshot pipeline stage failed op=$op stage=${adapter::class.qualifiedName}"
            cause = error
        }
    }

    private fun orderAdapters(adapters: List<AcPollSnapshotAdapter>): List<AcPollSnapshotAdapter> {
        val ordered = adapters.sortedWith(
            compareBy<AcPollSnapshotAdapter>(
                { it.order },
                { it::class.qualifiedName ?: it::class.simpleName.orEmpty() },
            ),
        )

        ordered
            .groupBy { it.order }
            .filterValues { it.size > 1 }
            .forEach { (order, collisions) ->
                logger.atWarn(RATE_LIMITED) {
                    message = "pipeline order collision pipeline=$PIPELINE_NAME order=$order " +
                        "stages=${collisions.joinToString { stageName(it) }}"
                }
            }

        return ordered
    }

    private fun stageName(adapter: AcPollSnapshotAdapter): String =
        adapter::class.qualifiedName ?: adapter::class.simpleName.orEmpty()

    private companion object {

        const val PIPELINE_NAME = "ac-poll-snapshot"
    }
}
