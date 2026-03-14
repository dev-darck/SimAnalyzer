package com.project.analyzer.base

import com.project.analyzer.deps
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private const val CORE_TEST_PROJECT_PATH = ":core:test"
private const val TEST_FIXTURES_API_ELEMENTS = "testFixturesApiElements"
private const val TEST_FIXTURES_RUNTIME_ELEMENTS = "testFixturesRuntimeElements"
private const val UI_TEST_ENABLED_EXTRA = "simAnalyzer.uiTestsEnabled"

internal fun Project.configureTest(configure: TestScope) {
    if (configure.enableUi) {
        extensions.extraProperties[UI_TEST_ENABLED_EXTRA] = true
    }

    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets {
            if (configure.enableUi) {
                commonTest.dependencies {
                    implementation(deps.compose.ui.test)
                }

                jvmMain.dependencies {
                    implementation(this@configureTest.coreTestDependency())
                }
            }

            jvmTest.dependencies {
                if (configure.enableUi) {
                    implementation(deps.compose.ui.test.junit4)
                    implementation(deps.navigation3.runtime)
                    compileOnly(this@configureTest.coreTestDependency(TEST_FIXTURES_API_ELEMENTS))
                    runtimeOnly(this@configureTest.coreTestDependency(TEST_FIXTURES_RUNTIME_ELEMENTS))
                }

                if (configure.enableUnit) {
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

private fun Project.coreTestDependency(configuration: String? = null) =
    dependencies.project(
        buildMap {
            put("path", CORE_TEST_PROJECT_PATH)
            configuration?.let { put("configuration", it) }
        },
    )
