package com.project.analyzer.ac.telemetry.impl.fallback.logfile

import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo

interface EvoFileInfoSource {

    fun poll(): EvoFileInfo
    fun clearPenalty()
    fun clear()
}
