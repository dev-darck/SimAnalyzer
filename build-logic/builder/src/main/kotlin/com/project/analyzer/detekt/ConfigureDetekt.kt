package com.project.analyzer.detekt

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.internal.Actions.with
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

internal fun Project.configureDetekt(block: DetektExtension.() -> Unit = {}) = with(project) {
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
    }
}
