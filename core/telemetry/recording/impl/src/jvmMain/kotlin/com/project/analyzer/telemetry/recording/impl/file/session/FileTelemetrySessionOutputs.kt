package com.project.analyzer.telemetry.recording.impl.file.session

import java.io.BufferedWriter
import java.io.DataOutputStream
import java.io.File

internal class FileTelemetrySessionOutputs(
    val metaFile: File,
    val dataOut: DataOutputStream,
    val indexOut: DataOutputStream,
    val eventsWriter: BufferedWriter,
)
