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
        projects.core.hud.api.jvmImpl
        projects.core.navigation.api.jvmImpl
        projects.core.theme.jvmImpl
        projects.core.ui.jvmImpl
        projects.core.telemetry.api.jvmImpl
        projects.core.preference.api.jvmImpl
        projects.feature.dev.calibration.jvmImpl
        projects.core.math.jvmImpl

        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
        lib.navigation3.runtime.jvmImpl
    }
}
