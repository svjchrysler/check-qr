plugins {
    alias(libs.plugins.checkqr.android.library.compose)
    alias(libs.plugins.checkqr.android.hilt)
}

android {
    namespace = "com.seef.checkqr.widget"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime.ktx)
}
