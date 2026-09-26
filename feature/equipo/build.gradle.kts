plugins {
    alias(libs.plugins.checkqr.android.feature)
}

android {
    namespace = "com.seef.checkqr.feature.equipo"
}

dependencies {
    // QR: generacion con ZXing, lectura con CameraX + ML Kit Barcode.
    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)
    implementation(libs.mlkit.barcode.scanning)
}
