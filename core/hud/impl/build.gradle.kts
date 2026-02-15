moduleImpl {
    compose()
    metro()
    logger()

    dependencies {
        projects.core.hud.api.jvmImpl
        projects.games.game.api.jvmImpl
        projects.games.game.impl.jvmImpl
        projects.core.di.api.jvmImpl
        projects.core.preference.api.jvmImpl
        projects.core.theme.jvmImpl

        lib.jna.base.jvmImpl
        lib.jna.platform.jvmImpl
        lib.metro.metrox.viewmodel.compose.jvmImpl
        lib.metro.metrox.viewmodel.base.jvmImpl
    }
}
