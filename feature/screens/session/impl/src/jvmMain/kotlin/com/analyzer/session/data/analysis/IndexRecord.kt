package com.analyzer.session.data.analysis

internal data class IndexRecord(
    val timestampNs: Long,
    val speedKmh: Float?,
    val lap: Int,
    val sector: Int,
    val flags: Int,
)
