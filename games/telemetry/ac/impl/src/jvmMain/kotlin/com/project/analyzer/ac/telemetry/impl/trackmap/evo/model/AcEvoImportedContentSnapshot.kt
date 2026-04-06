package com.project.analyzer.ac.telemetry.impl.trackmap.evo.model

import java.nio.file.Path

internal data class AcEvoImportedContentSnapshot(
    val assetsRoot: Path,
    val manifest: AcEvoImportedContentManifest
)
