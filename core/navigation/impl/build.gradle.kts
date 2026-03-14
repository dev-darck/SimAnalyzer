moduleImpl {
    metro()
    compose()
    resources()
    test {
        ui()
    }
    dependencies {
        projects.core.navigation.api.jvmImpl

        lib.metro.runtime.jvmImpl
        lib.navigation3.runtime.jvmImpl
        lib.navigation3.ui.jvmImpl
        lib.lifecycle.viewmodel.navigation3.jvmImpl
    }
}
