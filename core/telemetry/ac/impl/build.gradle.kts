moduleImpl {
    metro()
    proto()
    logger()

    dependencies {
        projects.core.telemetry.ac.api.jvmImpl
        projects.core.di.api.jvmImpl
        projects.core.math.jvmImpl

        lib.metro.runtime.jvmImpl
        lib.jna.base.jvmImpl
        lib.jna.platform.jvmImpl
    }
}
