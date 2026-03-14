moduleImpl {
    compose()
    metro()
    resources()
    logger()
    test {
        ui()
    }

    dependencies {
        projects.core.di.api.jvmImpl
        projects.core.leak.api.jvmImpl
        projects.core.navigation.api.jvmImpl
        projects.core.theme.jvmImpl
        projects.core.ui.jvmImpl
        projects.core.telemetry.api.jvmImpl
        projects.core.utils.jvmImpl
        projects.core.math.jvmImpl

        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
