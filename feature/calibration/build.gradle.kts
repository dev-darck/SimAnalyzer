moduleImpl {
    metro()
    compose()
    dependencies {
        projects.core.telemetry.ac.api.jvmImpl
        projects.core.navigation.api.jvmImpl
        projects.core.di.api.jvmImpl

        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
