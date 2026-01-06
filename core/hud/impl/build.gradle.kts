moduleImpl {
    compose()
    metro()

    dependencies {
        projects.core.hud.api.jvmImpl
        projects.core.di.api.jvmImpl
        projects.core.preference.api.jvmImpl

        lib.jna.base.jvmImpl
        lib.jna.platform.jvmImpl
        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
