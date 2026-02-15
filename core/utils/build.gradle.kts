moduleApi {
    metro()
    buildConfig {
        packageName = "utils"
    }
    dependencies {
        projects.core.di.api.jvmImpl

        lib.kotlin.logging.jvmImpl
        lib.sfl4j.api.jvmImpl
        lib.logback.classic.jvmImpl

        lib.jna.base.jvmImpl
        lib.jna.platform.jvmImpl
    }
}
