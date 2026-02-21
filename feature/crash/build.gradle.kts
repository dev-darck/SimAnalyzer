moduleImpl {
    compose()
    logger()
    metro()

    dependencies {
        projects.core.theme.jvmImpl
        projects.core.leak.api.jvmImpl

        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
