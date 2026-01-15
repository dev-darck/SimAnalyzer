moduleImpl {
    compose()
    metro()

    dependencies {
        projects.core.di.api.jvmImpl
        projects.core.navigation.api.jvmImpl

        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
