app {
    metro()
    dependencies {
        projects.core.di.api.jvmImpl
        projects.core.di.impl.jvmImpl
        projects.core.navigation.api.jvmImpl
        projects.core.navigation.impl.jvmImpl
        projects.core.telemetry.ac.api.jvmImpl
        projects.core.telemetry.ac.impl.jvmImpl
        projects.feature.calibration.jvmImpl

        lib.metro.runtime.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
        lib.metro.metrox.viewmodel.compose.jvmImpl
    }
}
