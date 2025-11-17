package org.gradle.kotlin.dsl

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency

/**
 * Enables a plugin in this project using a plugin alias defined
 * in the root project's version catalog. This is useful to ensure
 * an alias is synced with subproject applies.
 *
 * Traditional way:
 *
 * ```kotlin
 * plugins {
 *    alias(libs.plugins.some.plugin) apply false
 * }
 *
 * subprojects {
 *    apply(plugin = "some.plugin.id")
 * }
 * ```
 *
 * Using `enable`:
 *
 * ```kotlin
 * plugins {
 *   alias(libs.plugins.some.plugin) apply false
 * }
 *
 * subprojects {
 *   enable { libs.plugins.some.plugin }
 * }
 * ```
 *
 * This avoids hardcoding the abstracted plugin IDs in
 * multiple places.
 */
public fun Project.enable(
    block: Project.() -> Provider<*>,
): Unit = when (val alias = rootProject.block().get()) {
    is PluginDependency -> apply(plugin = alias.pluginId)
    else -> error("Cannot extract pluginId from alias: $alias")
}