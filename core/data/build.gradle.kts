plugins {
    alias(libs.plugins.checkqr.android.library)
    alias(libs.plugins.checkqr.android.hilt)
}

android {
    namespace = "com.seef.checkqr.core.data"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:database"))
    api(project(":core:datastore"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":capture:parser"))

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
}
