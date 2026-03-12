plugins {
    `kotlin-dsl`
}

dependencies {
    /**
     * workaround to make version catalog accessible in convention plugins
     * https://github.com/gradle/gradle/issues/15383
     */
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(gradleApi())
    implementation(localGroovy())
    implementation(libs.gradle.kotlin)
    implementation(libs.gradle.sqldelight)
    implementation(libs.gradle.compose.plugin)
    implementation(libs.gradle.detekt.plugin)
    implementation(libs.gradle.compose.compiler)
    implementation(libs.gradle.metro.plugin)
    implementation(projects.dependenciesWatcher)
}
