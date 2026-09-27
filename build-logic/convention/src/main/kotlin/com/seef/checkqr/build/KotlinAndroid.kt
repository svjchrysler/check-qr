package com.seef.checkqr.build

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Configuracion comun de Android + Kotlin para todos los modulos.
 *
 * En AGP 9 `CommonExtension` ya no es generica y solo expone getters, sin
 * bloques lambda, asi que todo se configura por propiedad.
 *
 * `targetSdk` no se toca aqui a proposito: en AGP 9 solo existe en modulos de
 * aplicacion, no en librerias, asi que lo pone el plugin de :app.
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    val javaVersion = libs.versionString("jvmTarget")

    commonExtension.compileSdk = libs.versionInt("compileSdk")
    commonExtension.defaultConfig.minSdk = libs.versionInt("minSdk")

    commonExtension.compileOptions.sourceCompatibility = JavaVersion.toVersion(javaVersion)
    commonExtension.compileOptions.targetCompatibility = JavaVersion.toVersion(javaVersion)
    commonExtension.compileOptions.encoding = "UTF-8"

    commonExtension.testOptions.unitTests.isIncludeAndroidResources = true
    commonExtension.testOptions.unitTests.isReturnDefaultValues = true

    commonExtension.lint.abortOnError = true
    commonExtension.lint.checkDependencies = true
    commonExtension.lint.warningsAsErrors = false

    // Kotlin puede emitir bytecode 21 desde cualquier JDK, pero javac no:
    // hay que darle un compilador 21 explicito o falla con "invalid source release".
    // Gradle lo baja solo via el foojay-resolver de settings.gradle.kts.
    val toolchains = extensions.getByType<JavaToolchainService>()
    tasks.withType<JavaCompile>().configureEach {
        javaCompiler.set(
            toolchains.compilerFor {
                languageVersion.set(JavaLanguageVersion.of(javaVersion))
            },
        )
    }

    // Gradle 9 falla si la tarea de test no descubre ninguno, y cuenta como
    // "fuentes presentes" los archivos generados (BuildConfig, R). En un
    // proyecto de 18 modulos hay varios que legitimamente todavia no tienen
    // tests, y esa falla no aporta nada: los tests que si existen igual corren.
    tasks.withType<Test>().configureEach {
        failOnNoDiscoveredTests.set(false)
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        jvmToolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion)) }
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(javaVersion))
            freeCompilerArgs.addAll("-Xconsistent-data-class-copy-visibility")
        }
    }

    dependencies {
        add("implementation", libs.findLibrary("kotlinx-coroutines-core").get())
        add("testImplementation", libs.findLibrary("junit").get())
        add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
    }
}
