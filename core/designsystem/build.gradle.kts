plugins {
    alias(libs.plugins.checkqr.android.library.compose)
}

android {
    namespace = "com.seef.checkqr.core.designsystem"
}

dependencies {
    // El design system formatea importes, asi que necesita el modulo de dinero.
    api(project(":core:common"))
    api(project(":core:model"))

    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material3.adaptive)
    api(libs.androidx.compose.material3.adaptive.layout)
    api(libs.androidx.compose.material3.windowSizeClass)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.window)
}
