plugins {
    alias(libs.plugins.checkqr.android.library)
    alias(libs.plugins.checkqr.android.hilt)
}

android {
    namespace = "com.seef.checkqr.voice"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))

    implementation(libs.androidx.lifecycle.service)
    // MediaSession real: legitima el tipo mediaPlayback del servicio en primer plano.
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)

    testImplementation(libs.mockk)
}
