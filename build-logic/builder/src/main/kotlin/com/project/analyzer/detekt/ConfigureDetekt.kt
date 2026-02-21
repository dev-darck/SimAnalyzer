package com.project.analyzer.detekt

import com.project.analyzer.applyPlugin
import com.project.analyzer.deps
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

internal fun Project.configureDetekt(block: DetektExtension.() -> Unit = {}) {
    with(pluginManager) {
        applyPlugin(deps.plugins.detekt)
    }

    dependencies.add("detektPlugins", deps.detekt.ktlint)

    extensions.configure<DetektExtension> {
        block()
        buildUponDefaultConfig.set(true)
        config.from(rootProject.files("config/detekt/detekt.yml"))

        source.from(
            "src/commonMain/kotlin",
            "src/commonTest/kotlin",
            "src/jvmMain/kotlin",
        )
    }

    tasks.withType<Detekt>().configureEach {
        exclude("**/build/**", "**/build/generated/**")
        autoCorrect.set(true)
        reports {
            html.required.set(true)
            sarif.required.set(true)
        }
    }
}
