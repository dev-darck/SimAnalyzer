subprojects {
    pluginManager.apply("convention-project-dsl")

    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(tasks.matching { it.name == "detekt" })
    }
}
