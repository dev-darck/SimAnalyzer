package com.project.analyzer.kmp

import org.gradle.api.Project
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.JvmApplication
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

internal fun Project.configureDesktop(scope: JvmApplication.() -> Unit = {}) {
    val compose = extensions.getByName("compose") as ComposeExtension
    compose.extensions.configure<DesktopExtension>("desktop") {
        application {
            mainClass = "com.project.analyzer.app.MainKt"
            nativeDistributions {
                targetFormats(
                    TargetFormat.Msi,
                    TargetFormat.Exe,
                    TargetFormat.AppImage
                )
                packageName = "SimAnalyzer"
                packageVersion = "0.0.1"

                windows { iconFile.set(rootProject.file("app-icons/app.ico")) }

                modules(
                    "java.instrument",
                    "java.naming",
                    "jdk.unsupported"
                )
            }

            buildTypes.release {
                proguard {
                    version.set("7.8.2")
                    configurationFiles.from("proguard-rules.pro")
                }
            }

            scope()
        }
    }
}
