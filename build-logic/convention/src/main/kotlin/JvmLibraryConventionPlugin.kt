import com.seef.checkqr.build.libs
import com.seef.checkqr.build.versionString
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Kotlin puro, sin nada de Android. Es lo que permite que los tests de
 * :capture:parser corran en la JVM sin emulador.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        val javaVersion = libs.versionString("jvmTarget")

        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion)) }
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(javaVersion))
                freeCompilerArgs.addAll("-Xconsistent-data-class-copy-visibility")
            }
        }

        tasks.withType(org.gradle.api.tasks.testing.Test::class.java).configureEach {
            useJUnit()
            failOnNoDiscoveredTests.set(false)
        }

        dependencies {
            add("implementation", libs.findLibrary("kotlinx-coroutines-core").get())
            add("testImplementation", libs.findLibrary("junit").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
        }
    }
}
