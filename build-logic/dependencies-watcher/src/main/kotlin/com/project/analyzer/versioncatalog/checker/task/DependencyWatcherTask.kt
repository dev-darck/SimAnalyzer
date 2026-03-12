package com.project.analyzer.versioncatalog.checker.task

import com.project.analyzer.versioncatalog.checker.catalog.CatalogLoader
import com.project.analyzer.versioncatalog.checker.cli.CatalogFileResolver
import com.project.analyzer.versioncatalog.checker.metadata.MetadataClient
import com.project.analyzer.versioncatalog.checker.metadata.TargetChecker
import com.project.analyzer.versioncatalog.checker.model.CommandMode
import com.project.analyzer.versioncatalog.checker.report.DependencyWatcherReporter
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Checks remote repositories for available dependency versions.")
internal abstract class DependencyWatcherTask : DefaultTask() {

    @get:Input
    abstract val command: Property<String>

    @get:Input
    abstract val catalogPath: Property<String>

    @get:Internal
    abstract val workspaceRootDir: DirectoryProperty

    @TaskAction
    fun runWatcher() {
        val diagnostics = mutableListOf<String>()
        val catalogFile = CatalogFileResolver.resolve(
            rawPath = catalogPath.get(),
            startDir = workspaceRootDir.asFile.get(),
        )
        val targets = CatalogLoader(diagnostics).load(catalogFile)
        val metadataClient = MetadataClient()
        val targetChecker = TargetChecker(metadataClient)
        val results = targets.map(targetChecker::check)
        val reporter = DependencyWatcherReporter(diagnostics)

        reporter.printSummary(
            catalogPath = catalogFile.path,
            results = results,
        )
        reporter.writeGithubOutputs(results)
        reporter.failIfNeeded(
            command = command.get().toCommandMode(),
            results = results,
        )
    }
}

private fun String.toCommandMode(): CommandMode = when (lowercase()) {
    "verify" -> CommandMode.Verify
    else -> CommandMode.Check
}
