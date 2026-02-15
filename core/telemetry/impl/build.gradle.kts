moduleImpl {
    metro()
    logger()

    dependencies {
        projects.core.telemetry.api.jvmImpl
        projects.core.preference.api.jvmImpl
        projects.core.di.api.jvmImpl
        projects.games.game.api.jvmImpl
        projects.games.game.impl.jvmImpl
        projects.core.utils.jvmImpl

        lib.metro.runtime.jvmImpl
    }
}
