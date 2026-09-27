package com.seef.checkqr.build

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** El catalogo `libs` del proyecto raiz, accesible desde los convention plugins. */
val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.versionInt(alias: String): Int =
    findVersion(alias).orElseThrow { IllegalStateException("Falta la version '$alias' en libs.versions.toml") }
        .requiredVersion.toInt()

fun VersionCatalog.versionString(alias: String): String =
    findVersion(alias).orElseThrow { IllegalStateException("Falta la version '$alias' en libs.versions.toml") }
        .requiredVersion
