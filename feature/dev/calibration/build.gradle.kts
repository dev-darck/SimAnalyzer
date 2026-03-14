moduleImpl {
    metro()
    compose()
    resources()
    test {
        ui()
    }
    dependencies {
        projects.core.telemetry.api.jvmImpl
        projects.games.telemetry.ac.api.jvmImpl
        projects.core.di.api.jvmImpl
        projects.core.leak.api.jvmImpl
        projects.core.hud.api.jvmImpl
        projects.core.math.jvmImpl
        projects.core.preference.api.jvmImpl
        projects.core.theme.jvmImpl
        projects.games.game.api.jvmImpl
        projects.core.ui.jvmImpl
        projects.core.utils.jvmImpl
        projects.feature.screens.trackMap.jvmImpl
        projects.feature.screens.trackMapBuilder.jvmImpl

        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
