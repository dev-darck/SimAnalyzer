moduleImpl {
    metro()
    proto()
    logger()
    buildConfig {
        packageName = "ac.telemetry.impl"
    }
    test {
        unit()
    }

    dependencies {
        projects.core.telemetry.runtime.api.jvmImpl
        projects.games.telemetry.ac.api.jvmImpl
        projects.core.telemetry.recording.api.jvmImpl
        projects.core.di.api.jvmImpl
        projects.core.leak.api.jvmImpl
        projects.core.math.jvmImpl
        projects.core.utils.jvmImpl
        projects.games.game.api.jvmImpl

        lib.metro.runtime.jvmImpl
        lib.jna.base.jvmImpl
        lib.jna.platform.jvmImpl
    }
}
