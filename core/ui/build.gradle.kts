moduleApi {
    compose()
    resources()
    buildConfig {
        packageName = "ui"
    }
    test {
        ui()
    }
    dependencies {
        projects.core.theme.jvmImpl
    }
}
