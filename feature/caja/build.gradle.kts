plugins {
    alias(libs.plugins.checkqr.android.feature)
}

android {
    namespace = "com.seef.checkqr.feature.caja"
}

dependencies {
    implementation(project(":voice"))
    implementation(project(":core:datastore"))
}
