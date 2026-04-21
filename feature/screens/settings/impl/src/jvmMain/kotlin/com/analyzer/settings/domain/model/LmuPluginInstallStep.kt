package com.analyzer.settings.domain.model

internal enum class LmuPluginInstallStep {
    ResolvingSource,
    DownloadingPackage,
    ValidatingPackage,
    ExtractingPlugin,
    WritingConfiguration,
    Finalizing,
}
