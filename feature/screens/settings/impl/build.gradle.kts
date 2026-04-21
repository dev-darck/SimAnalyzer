moduleImpl {
    compose()
    resources()
    metro()
    logger()
    test {
        ui()
    }
    buildConfig {
        packageName = "settings"
        common {
            string("ADDITIONAL_SETTINGS", "additionalSettings")
        }
    }

    dependencies {
        projects.core.di.api.jvmImpl
        projects.core.leak.api.jvmImpl
        projects.core.navigation.api.jvmImpl
        projects.core.theme.jvmImpl
        projects.core.ui.jvmImpl
        projects.games.game.api.jvmImpl
        projects.core.telemetry.runtime.api.jvmImpl
        projects.core.telemetry.recording.api.jvmImpl
        projects.core.utils.jvmImpl
        projects.core.math.jvmImpl
        projects.core.preference.api.jvmImpl
        projects.feature.screens.settings.api.jvmImpl
        projects.feature.screens.chooser.jvmImpl

        lib.kotlinx.serialization.json.jvmImpl
        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
