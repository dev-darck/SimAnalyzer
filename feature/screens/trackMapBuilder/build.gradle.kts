moduleImpl {
    compose()
    resources()
    metro()
    test {
        ui()
    }

    dependencies {
        projects.core.di.api.jvmImpl
        projects.core.navigation.api.jvmImpl
        projects.core.telemetry.runtime.api.jvmImpl
        projects.core.math.jvmImpl
        projects.core.leak.api.jvmImpl
        projects.core.theme.jvmImpl
        projects.core.ui.jvmImpl
        projects.core.utils.jvmImpl
        projects.games.game.api.jvmImpl
        projects.games.telemetry.ac.api.jvmImpl
        projects.feature.screens.trackMap.jvmImpl

        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
