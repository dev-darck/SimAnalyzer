package com.project.analyzer.ac.telemetry.impl.shm.structure

import com.project.analyzer.utils.shm.toBoolean as utilsToBoolean
import com.project.analyzer.utils.shm.toKString as utilsToKString
import com.project.analyzer.utils.shm.writeWString as utilsWriteWString

public fun CharArray.toKString(): String = this.utilsToKString()

public fun CharArray.writeWString(value: String): Unit = this.utilsWriteWString(value)

public fun Int.toBoolean(): Boolean = this.utilsToBoolean()
