package io.github.adelinosousa.gradle.plugins

import java.io.File
import kotlin.test.assertTrue
import kotlin.test.Test
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

class GhCliAuthSettingsPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }
    private val propertiesFile by lazy { projectDir.resolve("gradle.properties") }

    @Test fun `can run plugin`() {
        settingsFile.writeText("""
            plugins {
                id('io.github.adelinosousa.gradle.plugins.settings.gh-cli-auth')
            }
        """.trimIndent())
        buildFile.writeText("")
        propertiesFile.writeText("gh.cli.auth.github.org=test-org")

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("--stacktrace", "--info")
            .withGradleVersion("8.14.2")
            .buildAndFail()

        // Verify the result
        assertTrue(result.output.contains("GitHub CLI is not authenticated or does not have the required scopes"))
    }
}
