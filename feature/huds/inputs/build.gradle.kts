moduleImpl {
    metro()
    compose()
    resources()
    logger()
    proto()
    test {
        ui()
    }

    dependencies {
        projects.core.hud.api.jvmImpl
        projects.core.di.api.jvmImpl
        projects.core.leak.api.jvmImpl
        projects.core.theme.jvmImpl
        projects.core.telemetry.runtime.api.jvmImpl
        projects.core.math.jvmImpl
        projects.core.preference.api.jvmImpl
        projects.core.ui.jvmImpl

        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
