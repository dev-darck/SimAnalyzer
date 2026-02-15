import com.project.analyzer.scope.ProjectScope
import org.gradle.api.Project

private const val APP_PLUGIN_ID = "convention-compose-app"
private const val MODULE_API_PLUGIN_ID = "convention-module-api"
private const val MODULE_IMPL_PLUGIN_ID = "convention-module-impl"

internal fun Project.projectScope(block: ProjectScope.() -> Unit) {
    ProjectScope(this).block()
}

fun Project.app(block: ProjectScope.() -> Unit = {}) {
    pluginManager.apply(APP_PLUGIN_ID)
    projectScope(block)
}

fun Project.moduleApi(block: ProjectScope.() -> Unit = {}) {
    pluginManager.apply(MODULE_API_PLUGIN_ID)
    projectScope(block)
}

fun Project.moduleImpl(block: ProjectScope.() -> Unit = {}) {
    pluginManager.apply(MODULE_IMPL_PLUGIN_ID)
    projectScope(block)
}
