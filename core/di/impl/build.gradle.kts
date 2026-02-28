moduleImpl {
    metro()
    dependencies {
        projects.core.di.api.jvmImpl
        projects.core.navigation.api.jvmImpl
        projects.core.navigation.impl.jvmImpl
        projects.games.telemetry.ac.impl.jvmImpl
        projects.core.telemetry.impl.jvmImpl
        projects.games.telemetry.lmu.impl.jvmImpl
        projects.core.telemetry.api.jvmImpl
        projects.core.telemetry.recording.impl.jvmImpl
        projects.core.telemetry.recording.api.jvmImpl
        projects.feature.dev.calibration.jvmImpl
        projects.feature.dev.settings.jvmImpl
        projects.core.hud.api.jvmImpl
        projects.core.hud.impl.jvmImpl
        projects.games.game.api.jvmImpl
        projects.games.game.impl.jvmImpl
        projects.core.utils.jvmImpl
        projects.core.preference.api.jvmImpl
        projects.core.preference.impl.jvmImpl
        projects.core.leak.api.jvmImpl
        projects.core.leak.impl.jvmImpl
        projects.feature.huds.fuel.jvmImpl
        projects.feature.screens.setup.jvmImpl
        projects.feature.screens.settings.jvmImpl
        projects.feature.screens.live.jvmImpl
        projects.feature.screens.hudSettings.jvmImpl
        projects.feature.huds.inputs.jvmImpl
        projects.feature.screens.chooser.jvmImpl
        projects.feature.screens.session.impl.jvmImpl
        projects.feature.screens.sessionDetails.jvmImpl

        lib.metro.runtime.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
