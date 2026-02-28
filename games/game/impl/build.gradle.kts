moduleImpl {
    metro()
    dependencies {
        projects.games.game.api.jvmImpl
        projects.core.di.api.jvmImpl

        lib.jna.base.jvmImpl
        lib.jna.platform.jvmImpl
    }
}
