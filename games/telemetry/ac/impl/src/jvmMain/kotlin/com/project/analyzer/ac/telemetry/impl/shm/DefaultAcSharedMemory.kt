package com.project.analyzer.ac.telemetry.impl.shm

import com.project.analyzer.ac.telemetry.impl.shm.ac.AcLegacySharedMemoryReader
import com.project.analyzer.ac.telemetry.impl.shm.ace.AcEvoSharedMemoryReader
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.leak.api.LeakCanaryRuntime
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

@Inject
@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class, binding = binding<AcSharedMemory>())
class DefaultAcSharedMemory(names: AcShmNames) : AcSharedMemory {

    private val evo = AcEvoSharedMemoryReader(names.evo)
    private val legacy = AcLegacySharedMemoryReader(names.legacy)

    private var activeView: AcSharedMemoryView = AcUnknownSharedMemoryView

    override val layout: AcSharedMemoryLayout
        get() = activeView.layout

    override val view: AcSharedMemoryView
        get() = activeView

    override fun isAnyAttached(): Boolean = when (activeView.layout) {
        AcSharedMemoryLayout.ACEVO -> evo.isAnyAttached()
        AcSharedMemoryLayout.LEGACY -> legacy.isAnyAttached()
        AcSharedMemoryLayout.UNKNOWN -> evo.isAnyAttached() || legacy.isAnyAttached()
    }

    override fun readAll() {
        activeView = resolveActiveView()
        when (activeView.layout) {
            AcSharedMemoryLayout.ACEVO -> evo.readAll()
            AcSharedMemoryLayout.LEGACY -> legacy.readAll()
            AcSharedMemoryLayout.UNKNOWN -> Unit
        }
    }

    override fun close() {
        evo.close()
        legacy.close()
        activeView = AcUnknownSharedMemoryView

        LeakCanaryRuntime.watch(this, "AcSharedMemory")
    }

    private fun resolveActiveView(): AcSharedMemoryView {
        if (activeView.layout == AcSharedMemoryLayout.ACEVO) {
            return evo.view
        }
        if (activeView.layout == AcSharedMemoryLayout.LEGACY) {
            return legacy.view
        }

        evo.readAll()
        if (evo.isAnyAttached()) {
            return evo.view
        }

        legacy.readAll()
        if (legacy.isAnyAttached()) {
            return legacy.view
        }

        return AcUnknownSharedMemoryView
    }
}
