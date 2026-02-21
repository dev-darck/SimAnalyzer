package com.project.analyzer.telemetry.lmu.impl.mapper

import com.project.analyzer.math.Vec3

internal fun DoubleArray.toVec3(): Vec3 = Vec3(this[0].toFloat(), this[1].toFloat(), this[2].toFloat())

internal fun DoubleArray.toOriList(): List<Vec3> = listOf(
    Vec3(this[0].toFloat(), this[1].toFloat(), this[2].toFloat()),
    Vec3(this[3].toFloat(), this[4].toFloat(), this[5].toFloat()),
    Vec3(this[6].toFloat(), this[7].toFloat(), this[8].toFloat()),
)

internal fun Byte.toBoolean(): Boolean = this.toInt() != 0

internal fun Byte.toUnsignedInt(): Int = toInt() and BYTE_MASK

internal fun Short.toUnsignedInt(): Int = toInt() and SHORT_MASK

internal fun Int.toUnsignedLong(): Long = toLong() and INT_MASK

private const val BYTE_MASK = 0xFF
private const val SHORT_MASK = 0xFFFF
private const val INT_MASK = 0xFFFFFFFFL
