package org.gradle.kotlin.dsl

import org.gradle.api.initialization.Settings

/**
 * Includes a module located in the `modules` directory to create
 * a clean multi-module project structure.
 *
 * Examples:
 *   includeModule("app")           -> :app      -> modules/app
 *   includeModule("spec")          -> :spec     -> modules/spec
 *   includeModule("spec:client")   -> :spec:client -> modules/spec/client
 *
 * @param modulePath Gradle-style project path without leading ":".
 *                   Can be nested, e.g. "spec:client".
 * @param prefix An optional prefix to prepend to the leaf module name.
 */
public fun Settings.includeModule(modulePath: String, prefix: String = "") {
    // Normalize to remove any accidental leading colon
    val normalizedPath = modulePath.trimStart(':')
    val projectPath = ":$normalizedPath"

    include(projectPath)

    val module = project(projectPath)

    // Leaf name (e.g. "client" from "spec:client")
    val leafName = normalizedPath
        .substringAfterLast(':')
        .substringAfterLast('/')

    // Apply optional prefix to the leaf name
    module.name = prefix + leafName

    // Map "spec:client" -> modules/spec/client
    val modulesRoot = rootDir.toPath().resolve("modules")
    val projectDirPath = normalizedPath
        .split(':', '/')
        .fold(modulesRoot) { acc, segment -> acc.resolve(segment) }

    module.projectDir = projectDirPath.toFile()
}
