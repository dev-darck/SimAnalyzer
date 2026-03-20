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
        projects.core.telemetry.api.jvmImpl
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

tasks.withType<Test>().configureEach {
    if (name == "jvmTest") {
        // These tests allocate JNA-backed snapshots and are unstable when the whole suite
        // reuses one JVM for every class.
        forkEvery = 1
        maxHeapSize = "2048m"
        maxParallelForks = 1
    }
}
