moduleImpl {
    metro()
    logger()

    dependencies {
        projects.core.telemetry.api.jvmImpl
        projects.core.telemetry.recording.api.jvmImpl
        projects.games.telemetry.lmu.api.jvmImpl
        projects.core.di.api.jvmImpl
        projects.core.utils.jvmImpl
        projects.core.math.jvmImpl
        projects.games.game.api.jvmImpl

        lib.metro.runtime.jvmImpl
        lib.jna.base.jvmImpl
        lib.jna.platform.jvmImpl
    }
}
