moduleImpl {
    metro()
    dependencies {
        projects.core.di.api.jvmImpl
        projects.core.navigation.api.jvmImpl
        projects.core.navigation.impl.jvmImpl
        projects.core.telemetry.ac.impl.jvmImpl
        projects.core.telemetry.ac.api.jvmImpl
        projects.feature.calibration.jvmImpl
        projects.core.hud.api.jvmImpl
        projects.core.hud.impl.jvmImpl
        projects.core.utils.jvmImpl
        projects.core.preference.api.jvmImpl
        projects.core.preference.impl.jvmImpl
        projects.feature.huds.fuel.jvmImpl
        projects.feature.screens.session.jvmImpl
        projects.feature.screens.setup.jvmImpl
        projects.feature.screens.settings.jvmImpl
        projects.feature.screens.live.jvmImpl

        lib.metro.runtime.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
