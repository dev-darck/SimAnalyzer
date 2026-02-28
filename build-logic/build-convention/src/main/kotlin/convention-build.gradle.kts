import com.project.analyzer.buildconvention.configureRootVersioning

configureRootVersioning()

subprojects {
    version = rootProject.version
    pluginManager.apply("convention-project-dsl")

    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(tasks.matching { it.name == "detekt" })
    }
}
