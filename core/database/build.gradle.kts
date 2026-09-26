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

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
