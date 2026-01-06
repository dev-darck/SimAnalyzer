moduleImpl {
    metro()
    compose()
    dependencies {
        projects.core.telemetry.ac.api.jvmImpl
        projects.core.telemetry.ac.impl.jvmImpl
        projects.core.navigation.api.jvmImpl
        projects.core.di.api.jvmImpl
        projects.core.math.jvmImpl

        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
