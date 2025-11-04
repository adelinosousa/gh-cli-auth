package io.github.adelinosousa.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.extra

public class GhCliAuthSettingsPlugin : GhCliAuthBase(), Plugin<Settings> {
    override fun apply(settings: Settings) {
        val provider = settings.providers

        settings.gradle.extra.set(
            GH_EXTRA_TOKEN_KEY,
            provider.credentials.token
        )

        settings.pluginManagement.repositories.apply {
            addTrustedRepositoriesIfMissing()
            addUserConfiguredOrgGhPackagesRepository(provider)
        }

        @Suppress("UnstableApiUsage")
        settings.dependencyResolutionManagement.repositories.apply {
            addTrustedRepositoriesIfMissing()
            addUserConfiguredOrgGhPackagesRepository(provider)
        }
    }
}
