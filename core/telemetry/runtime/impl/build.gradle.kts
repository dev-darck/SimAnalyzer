moduleImpl {
    metro()
    logger()

    dependencies {
        projects.core.telemetry.runtime.api.jvmImpl
        projects.core.preference.api.jvmImpl
        projects.core.di.api.jvmImpl
        projects.core.leak.api.jvmImpl
        projects.games.game.api.jvmImpl
        projects.core.utils.jvmImpl

        lib.metro.runtime.jvmImpl
    }
}
