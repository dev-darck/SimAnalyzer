package com.project.analyzer.utils.steam

import java.nio.file.Path

public interface SteamInstallResolver {
    public fun findInstallDir(
        gameInstallDirName: String,
        steamRootProperty: String? = null,
        steamRootEnv: String? = null,
    ): Path?

    public fun findLibraries(steamRootProperty: String? = null, steamRootEnv: String? = null): Set<Path>
}
