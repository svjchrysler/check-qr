plugins {
    alias(libs.plugins.checkqr.android.library)
    alias(libs.plugins.checkqr.android.hilt)
}

android {
    namespace = "com.seef.checkqr.capture.listener"
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":capture:parser"))

    implementation(libs.androidx.lifecycle.service)

    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
