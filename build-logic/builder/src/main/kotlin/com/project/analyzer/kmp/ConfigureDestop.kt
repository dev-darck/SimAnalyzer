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
            mainClass = "com.project.analyzer.MainKt"

            nativeDistributions {
                targetFormats(
                    TargetFormat.Msi,
                    TargetFormat.Exe,
                    TargetFormat.AppImage
                )
                packageName = "com.project.analyzer"
                packageVersion = "1.0.0"

//                macOS { iconFile.set(rootProject.file("app-icons/app.icns")) }
//                windows { iconFile.set(rootProject.file("app-icons/app.ico")) }
//                linux { iconFile.set(rootProject.file("app-icons/app.png")) }

                modules(
                    "java.base",
                    "java.desktop",
                    "java.logging",
                    "java.management",
                    "jdk.unsupported"
                )
            }

            // ProGuard unsupported kotlin 2.3.0
            buildTypes.release {
                proguard {
                    isEnabled.set(false)
                }
            }

            scope()
        }
    }
}
