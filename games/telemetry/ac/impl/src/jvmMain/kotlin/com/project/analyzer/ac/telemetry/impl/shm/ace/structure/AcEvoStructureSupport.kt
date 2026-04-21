package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

import com.project.analyzer.utils.shm.toCString

internal fun ByteArray.toAceCString(): String = toCString()

internal fun Byte.toAceBool(): Boolean = toInt() != 0

internal fun Byte.toAceUInt8(): Int = toInt() and 0xFF

internal fun Byte.toAceInt8(): Int = toInt()

internal fun Short.toAceUInt16(): Int = toInt() and 0xFFFF

internal fun Int.toAceUInt32(): Long = toLong() and 0xFFFF_FFFFL
