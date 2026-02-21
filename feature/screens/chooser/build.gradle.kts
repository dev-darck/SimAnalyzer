moduleImpl {
    compose()
    metro()
    logger()

    dependencies {
        projects.core.ui.jvmImpl
        projects.core.theme.jvmImpl
        projects.core.di.api.jvmImpl
        projects.core.leak.api.jvmImpl
        projects.core.utils.jvmImpl

        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
