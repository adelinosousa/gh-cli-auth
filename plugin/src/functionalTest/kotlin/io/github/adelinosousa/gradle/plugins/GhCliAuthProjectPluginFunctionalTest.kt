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

    @Test fun `can run plugin`() {
        settingsFile.writeText("")
        buildFile.writeText("""
            plugins {
                id('io.github.adelinosousa.gradle.plugins.project.gh-cli-auth')
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("--stacktrace", "--info")
            .withGradleVersion("8.14.2")
            .buildAndFail()

        // Verify the result
        assertTrue(result.output.contains("Applying GitHubAuthPlugin to project"))
    }
}
