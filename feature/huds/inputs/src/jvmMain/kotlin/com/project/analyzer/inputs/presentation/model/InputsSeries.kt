package com.project.analyzer.inputs.presentation.model

import androidx.compose.runtime.Stable
import kotlin.math.min

@Stable
internal class InputsSeries(
    val capacity: Int
) {

    val throttle = FloatArray(capacity)
    val brake = FloatArray(capacity)
    val clutch = FloatArray(capacity)
    val steer = FloatArray(capacity)

    var head: Int = 0
        private set
    private var _size: Int = 0

    val size: Int get() = _size

    fun push(t: Float, b: Float, c: Float, s: Float) {
        throttle[head] = t
        brake[head] = b
        clutch[head] = c
        steer[head] = s

        head = (head + 1) % capacity
        _size = min(capacity, _size + 1)
    }

    fun resizedCopy(newCapacity: Int): InputsSeries {
        if (newCapacity == capacity) return this
        val out = InputsSeries(newCapacity)

        val toCopy = minOf(size, newCapacity)
        val startIndex = size - toCopy
        var i = 0
        forEachOldestToNewest { idx, t, b, c, s ->
            if (idx >= startIndex) {
                out.push(t, b, c, s)
                i++
            }
        }
        return out
    }

    inline fun forEachOldestToNewest(block: (i: Int, t: Float, b: Float, c: Float, s: Float) -> Unit) {
        val n = size
        if (n <= 0) return

        val start = if (n < capacity) 0 else head
        for (i in 0 until n) {
            val idx = (start + i) % capacity
            block(i, throttle[idx], brake[idx], clutch[idx], steer[idx])
        }
    }

    inline fun forEachOldestToNewest(step: Int, block: (i: Int, t: Float, b: Float, c: Float, s: Float) -> Unit) {
        if (step <= 1) {
            forEachOldestToNewest(block)
            return
        }

        val n = size
        if (n <= 0) return

        val start = if (n < capacity) 0 else head
        var i = 0
        while (i < n) {
            val idx = (start + i) % capacity
            block(i, throttle[idx], brake[idx], clutch[idx], steer[idx])
            i += step
        }

        if ((n - 1) % step != 0) {
            val idx = (start + (n - 1)) % capacity
            block(n - 1, throttle[idx], brake[idx], clutch[idx], steer[idx])
        }
    }
}
