plugins {
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.convention.project.dsl) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.ksp) apply false
    id("convention-build")
}

val uiTestEnabledExtra = "simAnalyzer.uiTestsEnabled"

tasks.register("ciJvmTest") {
    group = "verification"
    description = "Runs all jvmTest tasks across all subprojects."

    dependsOn(tasks.matching { it.name == "jvmTest" })
}

val ciUiTest = tasks.register("ciUiTest") {
    group = "verification"
    description = "Runs all UI jvmTest tasks across subprojects that enable test { ui() }."
}

gradle.projectsEvaluated {
    ciUiTest.configure {
        dependsOn(
            subprojects
                .filter { project ->
                    project.extensions.extraProperties.has(uiTestEnabledExtra) &&
                        project.extensions.extraProperties.get(uiTestEnabledExtra) == true
                }
                .map { project -> "${project.path}:jvmTest" },
        )
    }
}

tasks.register("detektAll") {
    group = "verification"
    description = "Runs all detekt tasks across all subprojects."

    dependsOn(tasks.matching { it.name == "detekt" })
}
