moduleApi {
    compose()
    resources()
    test {
        ui()
    }
    dependencies {
        lib.metro.runtime.jvmImpl
        lib.navigation3.ui.jvmImpl
        lib.navigation3.runtime.jvmImpl
        lib.kotlinx.serialization.json.jvmImpl
    }
}
