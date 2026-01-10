moduleImpl {
    compose()
    logger()
    metro()

    dependencies {
        projects.core.theme.jvmImpl

        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
