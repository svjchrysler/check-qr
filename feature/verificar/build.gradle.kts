plugins {
    alias(libs.plugins.checkqr.android.feature)
}

android {
    namespace = "com.seef.checkqr.feature.verificar"
}

dependencies {
    // Camara + OCR en el dispositivo para leer la captura del cliente.
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.compose)
    implementation(libs.mlkit.text.recognition)
    implementation(project(":capture:parser"))
}
