package com.project.analyzer.scope

import com.project.analyzer.ProjectDsl
import com.project.analyzer.base.TestScope
import com.project.analyzer.kmp.DesktopBuildConfigSpec
import com.project.analyzer.kmp.Fields

@ProjectDsl
class TestOptions internal constructor(
    internal val delegate: TestScope = TestScope(),
) {

    fun both() = delegate.both()

    fun unit() = delegate.unit()

    fun ui() = delegate.ui()
}

@ProjectDsl
class BuildConfigOptions internal constructor(
    internal val spec: DesktopBuildConfigSpec = DesktopBuildConfigSpec(),
) {

    var packageName: String?
        get() = spec.packageName
        set(value) {
            spec.packageName = value
        }

    fun common(block: BuildConfigFields.() -> Unit) {
        BuildConfigFields(spec.common).block()
    }

    fun debug(block: BuildConfigFields.() -> Unit) {
        BuildConfigFields(spec.debug).block()
    }

    fun release(block: BuildConfigFields.() -> Unit) {
        BuildConfigFields(spec.release).block()
    }
}

@ProjectDsl
class BuildConfigFields internal constructor(
    private val fields: Fields,
) {

    fun string(name: String, value: String) = fields.string(name, value)

    fun boolean(name: String, value: Boolean) = fields.boolean(name, value)

    fun int(name: String, value: Int) = fields.int(name, value)

    fun long(name: String, value: Long) = fields.long(name, value)
}
