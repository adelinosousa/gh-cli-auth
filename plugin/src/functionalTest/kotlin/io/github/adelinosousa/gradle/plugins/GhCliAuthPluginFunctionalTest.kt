package io.github.adelinosousa.gradle.plugins

import java.io.File
import kotlin.test.assertTrue
import kotlin.test.Test
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

class GhCliAuthPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    @Test fun `can run plugin`() {
        // Set up the test build
        settingsFile.writeText("")
        buildFile.writeText("""
            plugins {
                id('io.github.adelinosousa.gradle.plugins.project.gh-cli-auth')
            }
        """.trimIndent())

        val mockGhScript = createMockGhCliScript()
        val originalPath = System.getenv("PATH")
        val newPath = "${mockGhScript.parent}${File.pathSeparator}$originalPath"

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("-Pgh.cli.auth.github.org=test-org", "--stacktrace", "--info")
            .withEnvironment(mapOf("PATH" to newPath))
            .withGradleVersion("8.14.2")
            .build()

        // Verify the result
        assertTrue(result.output.contains("Applying GitHubAuthPlugin to project"))
    }

    private fun createMockGhCliScript(): File {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val binDir = File(projectDir, "bin").apply { mkdirs() }
        val scriptFileName = if (isWindows) "gh.bat" else "gh"
        val scriptFile = File(binDir, scriptFileName)

        val scriptContent = if (isWindows) {
            """
            @echo off
            if "%1" == "--version" (
                exit 0
            )
            if "%1" == "auth" if "%2" == "status" if "%3" == "--show-token" (
                echo github.com
                echo   ✓ Logged in to github.com account testuser (oauth_token:gho_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx)
                echo   ✓ Git operations for github.com configured to use https protocol.
                echo   ✓ Token: gho_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                echo   ✓ Token scopes: read:packages, repo, read:org
                exit 0
            )
            exit 1
            """.trimIndent()
        } else {
            """
            #!/bin/sh
            if [ "$1" = "--version" ]; then
                exit 0
            fi
            if [ "$1" = "auth" ] && [ "$2" = "status" ] && [ "$3" = "--show-token" ]; then
                echo "github.com"
                echo "  ✓ Logged in to github.com account testuser (oauth_token:gho_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx)"
                echo "  ✓ Git operations for github.com configured to use https protocol."
                echo "  ✓ Token: gho_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
                echo "  ✓ Token scopes: read:packages, repo, read:org"
                exit 0
            fi
            exit 1
            """.trimIndent()
        }

        scriptFile.writeText(scriptContent)
        scriptFile.setExecutable(true)
        return scriptFile
    }
}
