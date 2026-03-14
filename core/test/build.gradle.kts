moduleApi {
    compose()
    testFixtures()
    buildConfig {
        packageName = "test"
    }
    dependencies {
        lib.compose.ui.test.junit4.testFixturesImpl
    }
}
