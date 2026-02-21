moduleImpl {
    compose()
    metro()

    dependencies {
        projects.core.di.api.jvmImpl
        projects.core.leak.api.jvmImpl
        projects.core.navigation.api.jvmImpl
        projects.core.theme.jvmImpl
        projects.core.ui.jvmImpl
        projects.core.utils.jvmImpl
        projects.core.preference.api.jvmImpl
        projects.core.hud.api.jvmImpl
        projects.core.hud.impl.jvmImpl

        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
