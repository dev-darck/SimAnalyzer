moduleApi {
    compose()
    resources()
    test {
        ui()
    }
    dependencies {
        lib.metro.runtime.jvmImpl
        lib.kotlinx.serialization.json.jvmImpl
    }
}
