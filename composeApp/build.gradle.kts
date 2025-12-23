app {
    metro()
    dependencies {
        projects.core.di.api.jvmImpl
        projects.core.di.impl.jvmImpl
        projects.core.navigation.api.jvmImpl
        projects.core.navigation.impl.jvmImpl

        lib.metro.runtime.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
        lib.metro.metrox.viewmodel.compose.jvmImpl
    }
}
