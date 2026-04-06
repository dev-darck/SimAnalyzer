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

    val compose = extensions.getByName("compose") as? ComposeExtension
        ?: throw IllegalStateException("Compose extension not found")

    compose.extensions.configure<DesktopExtension>("desktop") {
        application {
            mainClass = "com.project.analyzer.app.MainKt"
            javaHome = javaHomeDir

            nativeDistributions {
                targetFormats(
                    TargetFormat.Msi,
                    TargetFormat.Exe,
                    TargetFormat.AppImage,
                )
                packageName = "SimAnalyzer"
                packageVersion = rootProject.appVersionBase()

                windows {
                    iconFile.set(rootProject.file("app-icons/app.ico"))
                    // App data lives next to the installed app, so keep installs per-user and validate custom paths in WiX.
                    perUserInstall = true
                    dirChooser = true
                }

                modules(
                    "java.instrument",
                    "java.naming",
                    "jdk.unsupported",
                    "jdk.management"
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

private fun Project.appVersionBase(): String =
    rootProject.extensions.extraProperties["appVersionBase"] as? String
        ?: error("appVersionBase is not configured in project properties")
