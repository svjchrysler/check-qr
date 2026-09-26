plugins {
    alias(libs.plugins.checkqr.android.library)
    alias(libs.plugins.checkqr.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.seef.checkqr.core.network"
    buildFeatures { buildConfig = true }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.okhttp.logging.interceptor)

    // Verificacion Ed25519 de las plantillas firmadas (API 26 no trae Ed25519).
    implementation(libs.tink.android)

    testImplementation(libs.okhttp.mockwebserver)
}
