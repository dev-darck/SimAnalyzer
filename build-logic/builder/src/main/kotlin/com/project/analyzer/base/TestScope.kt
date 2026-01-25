package com.project.analyzer.base

class TestScope {

    internal var enableUnit: Boolean = false
        private set
    internal var enableUi: Boolean = false
        private set

    internal fun both() {
        enableUnit = true
        enableUi = true
    }

    fun unit() {
        enableUnit = true
    }

    fun ui() {
        enableUi = true
    }
}
