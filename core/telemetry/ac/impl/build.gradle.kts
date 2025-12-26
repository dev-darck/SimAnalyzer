moduleImpl {
    metro()
    dependencies {
        projects.core.telemetry.ac.api.jvmImpl
        projects.core.di.api.jvmImpl

        lib.metro.runtime.jvmImpl
        lib.jna.base.jvmImpl
        lib.jna.platform.jvmImpl
    }
}
