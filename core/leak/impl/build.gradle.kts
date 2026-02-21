moduleImpl {
    metro()
    leakCanary()
    logger()
    buildConfig {
        packageName = "leak"
    }
    dependencies {
        projects.core.leak.api.jvmImpl
        projects.core.utils.jvmImpl
        projects.core.di.api.jvmImpl
    }
}
