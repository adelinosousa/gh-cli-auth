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
            .build()

        assertTrue(result.output.contains("Registering Maven GitHub repository for organization: test-org"))
    }

    @Test fun `correct number of repositories are configured for pluginManagement`() {
        settingsFile.writeText("""
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            
            plugins {
                id('io.github.adelinosousa.gradle.plugins.settings.gh-cli-auth')
            }
            
            dependencyResolutionManagement {
                repositories {
                    gradlePluginPortal()
                    google()
                    mavenCentral()
                }
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
            .build()

        assertTrue(result.output.contains("Adding Google repository"))
    }

    @Test fun `correct number of repositories are configured for dependencyResolutionManagement`() {
        settingsFile.writeText("""
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    google()
                    mavenCentral()
                }
            }
            
            plugins {
                id('io.github.adelinosousa.gradle.plugins.settings.gh-cli-auth')
            }
            
            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                }
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
            .build()

        assertTrue(result.output.contains("Adding Gradle Plugin Portal repository"))
    }

    @Test fun `runs plugin with custom env variable name`() {
        settingsFile.writeText(
            """
            plugins {
                id('io.github.adelinosousa.gradle.plugins.settings.gh-cli-auth')
            }
        """.trimIndent()
        )

        buildFile.writeText("")
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

    @Test fun `plugin shares token with other settings plugins`() {
        val tokenName = """gh-cli-auth-token"""
        val expectedToken = "ghp_exampletoken1234567890"

        settingsFile.writeText(
            """
            plugins {
                id('io.github.adelinosousa.gradle.plugins.settings.gh-cli-auth')
            }
    
            // a simple log to verify that the token is accessible from other setting plugins
            gradle.settingsEvaluated {
                def ghToken = gradle.ext.get("$tokenName")
                println("$tokenName: ${'$'}ghToken")
            }
        """.trimIndent()
        )

        buildFile.writeText("")

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
            .withEnvironment(mapOf("CUSTOM_ENV_VAR" to expectedToken))
            .build()

        assertTrue(result.output.contains("$tokenName: $expectedToken"))
    }
}
