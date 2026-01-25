package com.project.analyzer.base

import com.project.analyzer.deps
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureTest(configure: TestScope.() -> Unit) {
    val scope = TestScope().apply(configure)

    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets {
            jvmTest.dependencies {
                if (scope.enableUi) {
                    implementation(deps.compose.ui.test.junit4)
                }

                if (scope.enableUnit) {
                    implementation(deps.junit)
                    implementation(deps.kotlinx.coroutines.test)
                    implementation(deps.kotlin.test)
                    implementation(deps.kotlin.testJunit)
                    implementation(deps.mockk)
                }

                implementation(deps.junit.platform.launcher)
            }
        }
    }
}
