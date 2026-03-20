package com.analyzer.session.data.repository

internal const val META_FILE_NAME = "session.json"
internal const val INDEX_FILE_NAME = "index.bin"
internal const val COMPRESSION_GZIP = "gzip"
internal const val INDEX_MAGIC = 0x53414958
internal const val INDEX_RECORD_SIZE = 64
internal const val SESSION_BUNDLE_GAP_MAX_MS = 20 * 60 * 1000L
internal const val DETAIL_PLACEHOLDER_MAX_FRAMES = 5L
