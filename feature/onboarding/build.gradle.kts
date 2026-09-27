plugins {
    alias(libs.plugins.checkqr.android.feature)
}

android {
    namespace = "com.seef.checkqr.feature.onboarding"
}

dependencies {
    implementation(project(":voice"))
    implementation(project(":capture:listener"))
    implementation(project(":core:datastore"))
}
