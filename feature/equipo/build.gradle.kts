plugins {
    alias(libs.plugins.checkqr.android.feature)
    // La carga del QR de invitacion viaja como JSON.
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.seef.checkqr.feature.equipo"
}

dependencies {
    // QR: generacion con ZXing, lectura con CameraX + ML Kit Barcode.
    implementation(libs.zxing.core)
    implementation(project(":core:datastore"))
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    // PreviewView y LifecycleCameraController viven en camera-view.
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.compose)
    implementation(libs.mlkit.barcode.scanning)
}
