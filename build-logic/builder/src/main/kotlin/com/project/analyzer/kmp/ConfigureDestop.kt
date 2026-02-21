package com.project.analyzer.kmp

import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.JvmApplication
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

internal fun Project.configureDesktop(scope: JvmApplication.() -> Unit = {}) {
    val javaToolchains = extensions.getByType<JavaToolchainService>()
    val javaHomeDir = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }.get().metadata.installationPath.asFile.absolutePath

    val compose = extensions.getByName("compose") as ComposeExtension
    compose.extensions.configure<DesktopExtension>("desktop") {
        application {
            mainClass = "com.project.analyzer.app.MainKt"
            javaHome = javaHomeDir

            nativeDistributions {
                targetFormats(
                    TargetFormat.Msi, TargetFormat.Exe, TargetFormat.AppImage
                )
                packageName = "SimAnalyzer"
                packageVersion = "0.0.1"

                windows {
                    iconFile.set(rootProject.file("app-icons/app.ico"))
                }

                modules(
                    "java.instrument", "java.naming", "jdk.unsupported"
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
