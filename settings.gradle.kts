pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CheckQr"

include(":app")

// Nucleo. :core:model, :core:common y :capture:parser son Kotlin puro a proposito.
include(":core:model")
include(":core:common")
include(":core:database")
include(":core:datastore")
include(":core:network")
include(":core:data")
include(":core:designsystem")

// Captura de avisos del banco.
include(":capture:listener")
include(":capture:parser")

// Voz, sincronizado y widget.
include(":voice")
include(":sync")
include(":widget")

// Pantallas.
include(":feature:onboarding")
include(":feature:caja")
include(":feature:equipo")
include(":feature:cuadre")
include(":feature:verificar")
