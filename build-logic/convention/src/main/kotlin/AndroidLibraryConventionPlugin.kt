import com.android.build.api.dsl.LibraryExtension
import com.seef.checkqr.build.configureKotlinAndroid
import com.seef.checkqr.build.libs
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
        }

        dependencies {
            add("implementation", libs.findLibrary("androidx-core-ktx").get())
            add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())
        }
    }
}
