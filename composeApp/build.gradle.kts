app {
    metro()
    buildConfig {
        packageName = "app"
    }
    logger()
    resources()

    dependencies {
        projects.core.di.api.jvmImpl
        projects.core.leak.api.jvmImpl
        projects.core.di.impl.jvmImpl
        projects.core.navigation.api.jvmImpl
        projects.core.navigation.impl.jvmImpl
        projects.core.hud.api.jvmImpl
        projects.core.hud.impl.jvmImpl
        projects.core.preference.api.jvmImpl
        projects.core.theme.jvmImpl
        projects.feature.screens.settings.api.jvmImpl
        projects.feature.screens.sessionDetails.jvmImpl
        projects.feature.crash.jvmImpl
        projects.core.ui.jvmImpl
        projects.core.utils.jvmImpl
        projects.games.game.api.jvmImpl

        lib.metro.runtime.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.jna.platform.jvmImpl
        lib.jna.base.jvmImpl
    }
}
