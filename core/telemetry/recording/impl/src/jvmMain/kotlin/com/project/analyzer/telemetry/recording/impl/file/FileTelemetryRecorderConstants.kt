package com.project.analyzer.telemetry.recording.impl.file

import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_COMPRESSION_GZIP
import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_FILE_MAGIC
import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_FRAMES_FILE_NAME
import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_INDEX_FILE_NAME
import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_INDEX_MAGIC
import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_INDEX_RECORD_SIZE
import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_META_FILE_NAME
import kotlin.time.Duration.Companion.seconds

internal const val FILE_MAGIC = TELEMETRY_SESSION_FILE_MAGIC
internal const val FILE_VERSION = 2
internal const val INDEX_MAGIC = TELEMETRY_SESSION_INDEX_MAGIC
internal const val INDEX_VERSION = 1
internal const val INDEX_RECORD_SIZE = TELEMETRY_SESSION_INDEX_RECORD_SIZE
internal const val INDEX_FLAGS = 0
internal val INDEX_FIELD_NAMES = listOf(
    "pos_x",
    "pos_z",
    "heading_rad",
    "speed_kmh",
    "track_pos",
    "lap",
    "sector",
    "flags",
)
internal const val FRAME_HEADER_FIXED_SIZE = 16
internal const val FRAME_RECORD_HEADER_SIZE = 21
internal const val FRAMES_FILE_NAME = TELEMETRY_SESSION_FRAMES_FILE_NAME
internal const val INDEX_FILE_NAME = TELEMETRY_SESSION_INDEX_FILE_NAME
internal const val META_FILE_NAME = TELEMETRY_SESSION_META_FILE_NAME
internal const val EVENTS_FILE_NAME = "events.jsonl"
internal const val COMPRESSION_GZIP = TELEMETRY_SESSION_COMPRESSION_GZIP
internal val FLUSH_INTERVAL_NS = 1.seconds.inWholeNanoseconds
internal const val DEFAULT_QUEUE_CAPACITY = 512

internal const val EVENT_STARTED = "start"
internal const val EVENT_UPDATED = "update"
internal const val EVENT_PAUSED = "pause"
internal const val EVENT_RESUMED = "resume"
internal const val EVENT_ENDED = "end"
