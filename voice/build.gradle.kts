plugins {
    alias(libs.plugins.checkqr.android.library)
    alias(libs.plugins.checkqr.android.hilt)
}

android {
    namespace = "com.seef.checkqr.voice"
    buildFeatures { buildConfig = false }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))

    // Se usa la MediaSession de la plataforma (android.media.session) y no la de
    // media3: media3 exige una implementacion completa de Player para algo que
    // aqui solo necesita informar un estado de reproduccion activo.

    testImplementation(libs.mockk)
}
