plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.toml.parser)
    implementation("org.apache.maven:maven-artifact:3.9.9")
}
