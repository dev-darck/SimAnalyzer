package com.project.analyzer.telemetry.recording.impl.file

import kotlin.time.Duration.Companion.seconds

internal const val FILE_MAGIC = 0x5341544D
internal const val FILE_VERSION = 2
internal const val INDEX_MAGIC = 0x53414958
internal const val INDEX_VERSION = 1
internal const val INDEX_RECORD_SIZE = 64
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
internal const val FRAMES_FILE_NAME = "frames.bin"
internal const val INDEX_FILE_NAME = "index.bin"
internal const val META_FILE_NAME = "session.json"
internal const val EVENTS_FILE_NAME = "events.jsonl"
internal const val COMPRESSION_GZIP = "gzip"
internal val FLUSH_INTERVAL_NS = 1.seconds.inWholeNanoseconds
internal const val DEFAULT_QUEUE_CAPACITY = 512

internal const val EVENT_STARTED = "start"
internal const val EVENT_UPDATED = "update"
internal const val EVENT_PAUSED = "pause"
internal const val EVENT_RESUMED = "resume"
internal const val EVENT_ENDED = "end"
