moduleImpl {
    metro()

    dependencies {
        projects.core.di.api.jvmImpl
        projects.core.telemetry.analysis.api.jvmImpl
        projects.core.telemetry.recording.api.jvmImpl
        projects.core.utils.jvmImpl

        lib.metro.runtime.jvmImpl
    }
}
