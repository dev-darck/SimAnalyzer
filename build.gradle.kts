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
}

subprojects {
    pluginManager.apply("convention-project-dsl")
}
