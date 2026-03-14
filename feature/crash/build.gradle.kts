moduleImpl {
    compose()
    resources()
    logger()
    metro()
    test {
        ui()
    }

    dependencies {
        projects.core.theme.jvmImpl
        projects.core.ui.jvmImpl
        projects.core.leak.api.jvmImpl

        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
