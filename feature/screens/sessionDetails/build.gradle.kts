moduleImpl {
    compose()
    resources()
    metro()
    test {
        ui()
    }

    dependencies {
        projects.core.di.api.jvmImpl
        projects.core.leak.api.jvmImpl
        projects.core.navigation.api.jvmImpl
        projects.core.telemetry.recording.api.jvmImpl
        projects.core.theme.jvmImpl
        projects.core.ui.jvmImpl
        projects.core.utils.jvmImpl
        projects.feature.screens.chooser.jvmImpl
        projects.feature.screens.session.api.jvmImpl

        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
