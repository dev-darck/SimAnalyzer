package com.project.analyzer.telemetry.recording.api.file

public const val TELEMETRY_SESSION_META_FILE_NAME: String = "session.json"
public const val TELEMETRY_SESSION_FRAMES_FILE_NAME: String = "frames.bin"
public const val TELEMETRY_SESSION_INDEX_FILE_NAME: String = "index.bin"
public const val TELEMETRY_SESSION_COMPRESSION_GZIP: String = "gzip"
public const val TELEMETRY_SESSION_FILE_MAGIC: Int = 0x5341544D
public const val TELEMETRY_SESSION_INDEX_MAGIC: Int = 0x53414958
public const val TELEMETRY_SESSION_INDEX_RECORD_SIZE: Int = 64
