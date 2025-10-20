package io.github.adelinosousa.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging
import org.gradle.kotlin.dsl.extra

internal class MockSettingsPlugin : Plugin<Settings> {
    private companion object {
        private val logger = Logging.getLogger(MockSettingsPlugin::class.java)
    }

    override fun apply(settings: Settings) {
        if (settings.gradle.extra.has(Config.EXTRA_TOKEN_NAME)) {
            logger.info("MockSettingsPlugin applied successfully and found the token: ${settings.gradle.extra[Config.EXTRA_TOKEN_NAME]}")
            return
        }
    }
}