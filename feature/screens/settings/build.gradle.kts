moduleImpl {
    compose()
    metro()
    buildConfig {
        packageName = "settings"
        common {
            string("ADDITIONAL_SETTINGS", "additionalSettings")
        }
    }

    dependencies {
        projects.core.di.api.jvmImpl
        projects.core.navigation.api.jvmImpl
        projects.core.theme.jvmImpl
        projects.core.ui.jvmImpl
        projects.core.telemetry.ac.api.jvmImpl
        projects.core.utils.jvmImpl
        projects.core.math.jvmImpl
        projects.core.preference.api.jvmImpl

        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
