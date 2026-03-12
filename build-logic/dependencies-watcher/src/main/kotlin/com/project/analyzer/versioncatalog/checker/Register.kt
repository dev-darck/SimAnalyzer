package com.project.analyzer.versioncatalog.checker

import com.project.analyzer.versioncatalog.checker.model.DependencyWatcherConstants
import com.project.analyzer.versioncatalog.checker.task.DependencyWatcherTask
import org.gradle.api.Project

fun Project.registerDependencyWatcher() {
    val root = rootProject
    val catalogPath = root.providers.gradleProperty("dependencyWatcher.catalog")
        .orElse(DependencyWatcherConstants.DEFAULT_TOML_PATH)

    if (root.tasks.findByName("dependencyWatcher") == null) {
        root.tasks.register("dependencyWatcher", DependencyWatcherTask::class.java) {
            group = "verification"
            description = "Checks gradle/libs.versions.toml for available dependency updates."
            command.set("check")
            this.catalogPath.set(catalogPath)
            workspaceRootDir.set(root.layout.projectDirectory)
        }
    }

    if (root.tasks.findByName("dependencyWatcherVerify") == null) {
        root.tasks.register("dependencyWatcherVerify", DependencyWatcherTask::class.java) {
            group = "verification"
            description = "Fails if gradle/libs.versions.toml has available dependency updates."
            command.set("verify")
            this.catalogPath.set(catalogPath)
            workspaceRootDir.set(root.layout.projectDirectory)
        }
    }
}
