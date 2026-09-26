plugins {
    alias(libs.plugins.checkqr.android.library)
    alias(libs.plugins.checkqr.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.seef.checkqr.core.network"
    buildFeatures { buildConfig = true }

    defaultConfig {
        // Clave publica Ed25519 (32 bytes en base64) con la que se verifican las
        // plantillas firmadas. Vacia = el verificador rechaza todo y la app se
        // queda con las plantillas empaquetadas, que es el fallo seguro.
        // PENDIENTE: poner la clave real al crear el par en Secret Manager.
        buildConfigField("String", "CLAVE_PUBLICA_PLANTILLAS", "\"\"")

        // URL del backend. Se sobreescribe por variante cuando exista el
        // servicio desplegado.
        buildConfigField("String", "URL_BASE", "\"https://api.checkqr.app/\"")
    }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    // Por la interfaz VerificadorDePlantillas, que vive en el modulo puro para
    // que el parser y sus tests no dependan de Android.
    implementation(project(":capture:parser"))

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.okhttp.logging.interceptor)

    // Verificacion Ed25519 de las plantillas firmadas (API 26 no trae Ed25519).
    implementation(libs.tink.android)

    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
