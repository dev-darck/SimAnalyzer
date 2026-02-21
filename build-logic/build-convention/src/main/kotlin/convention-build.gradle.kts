subprojects {
    pluginManager.apply("convention-project-dsl")

    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(tasks.matching { it.name == "detekt" })
    }
}

tasks.register("ciJvmTest") {
    group = "verification"
    description = "Runs all jvmTest tasks across all subprojects."

    subprojects.forEach { p ->
        dependsOn(p.tasks.matching { it.name == "jvmTest" })
    }
}

tasks.register("detektAll") {
    group = "verification"
    description = "Runs all detekt tasks across all subprojects."

    subprojects.forEach { p ->
        dependsOn(p.tasks.matching { it.name == "detekt" })
    }
}
