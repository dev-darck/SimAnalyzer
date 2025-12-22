import com.project.analyzer.applyPlugin
import com.project.analyzer.deps
import com.project.analyzer.dsl.AppModuleExtension
import com.project.analyzer.dsl.ModuleExtension
import org.gradle.api.Project

fun Project.app(block: AppModuleExtension.() -> Unit = {}) {
    pluginManager.applyPlugin(deps.plugins.convention.app.plugin)

    val extension = extensions.findByType(AppModuleExtension::class.java)
        ?: extensions.create("app", AppModuleExtension::class.java, this)

    extension.block()
}

fun Project.moduleApi(block: ModuleExtension.() -> Unit = {}) {
    pluginManager.applyPlugin(deps.plugins.convention.module.api.plugin)

    val extension = extensions.findByType(ModuleExtension::class.java)
        ?: extensions.create("moduleApi", ModuleExtension::class.java, this)

    extension.block()
}

fun Project.moduleImpl(block: ModuleExtension.() -> Unit = {}) {
    pluginManager.applyPlugin(deps.plugins.convention.module.impl.plugin)

    val extension = extensions.findByType(ModuleExtension::class.java)
        ?: extensions.create("moduleImpl", ModuleExtension::class.java, this)

    extension.block()
}
