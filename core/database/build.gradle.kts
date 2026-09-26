plugins {
    alias(libs.plugins.checkqr.android.library)
    alias(libs.plugins.checkqr.android.hilt)
    alias(libs.plugins.checkqr.android.room)
}

android {
    namespace = "com.seef.checkqr.core.database"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))

    // Room corre bien bajo Robolectric, y las dos invariantes que mas importan
    // (deduplicacion y reclamo unico de comprobante) se verifican asi en cada
    // `./gradlew test`, sin depender de que alguien arranque un emulador.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
