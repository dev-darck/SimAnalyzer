package com.project.analyzer.scope

import com.project.analyzer.ProjectDsl
import com.project.analyzer.base.TestScope
import com.project.analyzer.base.configureLeakCanaryJvm
import com.project.analyzer.base.configureMetro
import com.project.analyzer.base.configureTest
import com.project.analyzer.kmp.DesktopBuildConfigSpec
import com.project.analyzer.kmp.Fields
import com.project.analyzer.kmp.configureDesktopBuildConfig
import com.project.analyzer.kmp.configureLogger
import com.project.analyzer.kmp.configureProtoSerializer
import dev.zacsweers.metro.gradle.MetroPluginExtension

internal fun ProjectScope.metroImpl(block: MetroPluginExtension.() -> Unit = {}) {
    configureMetro(block)
}

internal fun ProjectScope.protoImpl() {
    configureProtoSerializer()
}

internal fun ProjectScope.loggerImpl() {
    configureLogger()
}

internal fun ProjectScope.leakCanaryImpl() {
    configureLeakCanaryJvm()
}

internal fun ProjectScope.testImpl(config: TestOptions.() -> Unit = { both() }) {
    val options = TestOptions().apply(config)
    configureTest {
        if (options.delegate.enableUnit) unit()
        if (options.delegate.enableUi) ui()
    }
}

internal fun ProjectScope.buildConfigImpl(block: BuildConfigOptions.() -> Unit) {
    val options = BuildConfigOptions().apply(block)
    configureDesktopBuildConfig(options.spec)
}

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
