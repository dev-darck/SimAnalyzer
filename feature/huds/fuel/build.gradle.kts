moduleImpl {
    metro()
    compose()
    dependencies {
        projects.core.hud.api.jvmImpl
        projects.core.di.api.jvmImpl
        projects.core.theme.jvmImpl

        lib.metro.runtime.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
