package org.gradle.kotlin.dsl

import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency

/**
 * Converts a [Provider] of [PluginDependency] to a [Provider] of [String],
 * mapping to Gradle dependency notation to depend on the plugin directly.
 */
public fun Provider<PluginDependency>.asDependency(): Provider<String> =
    this.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
