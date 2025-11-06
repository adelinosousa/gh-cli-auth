package org.gradle.kotlin.dsl

import org.gradle.api.initialization.Settings

/**
 * Includes a module located in the `modules` directory to create
 * a clean multi-module project structure.
 *
 * @param moduleDir The name of the module directory inside `modules`.
 * @param prefix An optional prefix to prepend to the module name.
 */
public fun Settings.includeModule(moduleDir: String, prefix: String = "") {
    val path = ":$moduleDir"
    include(path)

    val module = project(path)
    module.name = prefix + moduleDir
    module.projectDir =
        settings.rootDir
            .toPath()
            .resolve("modules")
            .resolve(moduleDir)
            .toFile()
}
