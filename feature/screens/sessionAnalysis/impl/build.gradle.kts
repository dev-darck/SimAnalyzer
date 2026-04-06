moduleImpl {
    compose()
    resources()
    metro()
    logger()
    test {
        unit()
    }

    dependencies {
        projects.core.di.api.jvmImpl
        projects.core.leak.api.jvmImpl
        projects.core.navigation.api.jvmImpl
        projects.core.telemetry.analysis.api.jvmImpl
        projects.core.telemetry.recording.api.jvmImpl
        projects.core.theme.jvmImpl
        projects.core.ui.jvmImpl
        projects.core.utils.jvmImpl
        projects.games.telemetry.ac.api.jvmImpl

        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
