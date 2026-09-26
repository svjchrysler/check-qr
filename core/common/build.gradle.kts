plugins {
    alias(libs.plugins.checkqr.jvm.library)
}

dependencies {
    api(project(":core:model"))
}
