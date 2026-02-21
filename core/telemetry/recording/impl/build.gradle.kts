moduleImpl {
    metro()
    logger()
    test {
        unit()
    }

    dependencies {
        projects.core.telemetry.recording.api.jvmImpl
        projects.core.telemetry.api.jvmImpl
        projects.core.math.jvmImpl
        projects.core.preference.api.jvmImpl
        projects.core.utils.jvmImpl
        projects.core.di.api.jvmImpl
        projects.core.leak.api.jvmImpl

        lib.metro.runtime.jvmImpl
    }
}
