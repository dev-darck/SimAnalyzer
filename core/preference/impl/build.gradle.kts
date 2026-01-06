moduleImpl {
    metro()
    dependencies {
        projects.core.preference.api.jvmImpl
        projects.core.utils.jvmImpl

        lib.datastore.preferences.core.jvmImpl
    }
}
