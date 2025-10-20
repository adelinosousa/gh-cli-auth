package io.github.adelinosousa.gradle.plugins

import java.io.File
import kotlin.test.assertTrue
import kotlin.test.Test
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

class GhCliAuthProjectPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }
    private val propertiesFile by lazy { projectDir.resolve("gradle.properties") }

    @Test fun `can run plugin`() {
        settingsFile.writeText("")
        buildFile.writeText("""
            plugins {
                id('io.github.adelinosousa.gradle.plugins.project.gh-cli-auth')
            }
        """.trimIndent())
        propertiesFile.writeText("gh.cli.auth.github.org=test-org")

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("--stacktrace", "--info")
            .withGradleVersion("8.14.2")
            .build()

        assertTrue(result.output.contains("Registering Maven GitHub repository for organization: test-org"))
    }

    @Test fun `runs plugin with custom env variable name`() {
        settingsFile.writeText("")
        buildFile.writeText("""
            plugins {
                id('io.github.adelinosousa.gradle.plugins.project.gh-cli-auth')
            }
        """.trimIndent())
        propertiesFile.writeText("""
            gh.cli.auth.github.org=test-org
            gh.cli.auth.env.name=CUSTOM_ENV_VAR
        """.trimIndent())

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("--stacktrace", "--info")
            .withGradleVersion("8.14.2")
            .withEnvironment(mapOf("CUSTOM_ENV_VAR" to "ghp_exampletoken1234567890"))
            .build()

        assertTrue(result.output.contains("Registering Maven GitHub repository for organization: test-org"))
    }
}
