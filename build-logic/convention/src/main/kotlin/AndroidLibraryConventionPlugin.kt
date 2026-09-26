import com.android.build.api.dsl.LibraryExtension
import com.seef.checkqr.build.configureKotlinAndroid
import com.seef.checkqr.build.libs
import com.seef.checkqr.build.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            // En AGP 9 las librerias no declaran targetSdk; lo hereda la app.
            testOptions.animationsDisabled = true
            // Los tests de JVM corren con Robolectric, que todavia no trae el
            // SDK 37: sin esto fallan al arrancar con "targetSdkVersion=37 >
            // maxSdkVersion=36". AGP solo acepta este ajuste en librerias.
            testOptions.targetSdk = libs.versionInt("robolectricSdk")
        }

        dependencies {
            add("implementation", libs.findLibrary("androidx-core-ktx").get())
            add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())
        }
    }
}
