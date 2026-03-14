moduleImpl {
    compose()
    resources()
    metro()
    test {
        ui()
    }

    dependencies {
        projects.core.di.api.jvmImpl
        projects.core.theme.jvmImpl
        projects.core.ui.jvmImpl
        projects.core.math.jvmImpl
        projects.games.telemetry.ac.api.jvmImpl

    }
}
