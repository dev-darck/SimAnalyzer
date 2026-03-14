package com.project.analyzer.base

import com.project.analyzer.ProjectDsl

@ProjectDsl
class TestScope {

    var enableUnit: Boolean = false
        private set
    var enableUi: Boolean = false
        private set

    fun both() {
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
