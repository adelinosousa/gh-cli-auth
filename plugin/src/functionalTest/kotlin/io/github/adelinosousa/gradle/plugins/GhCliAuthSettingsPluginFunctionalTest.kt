package io.github.adelinosousa.gradle.plugins

import io.github.adelinosousa.gradle.plugins.GhCliAuthBase.Companion.DEFAULT_TOKEN_ENV_KEY
import io.github.adelinosousa.gradle.plugins.GhCliAuthBase.Companion.DEFAULT_TOKEN_PROPERTY_KEY
import io.github.adelinosousa.gradle.plugins.GhCliAuthBase.Companion.GH_ENV_KEY_SETTER_PROPERTY
import io.github.adelinosousa.gradle.plugins.GhCliAuthBase.Companion.GH_PROPERTY_KEY_SETTER_PROPERTY
import io.github.adelinosousa.gradle.plugins.support.GhCliAuthFake
import io.github.adelinosousa.gradle.plugins.support.GhCliAuthFunctionalTestSetup
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class GhCliAuthSettingsPluginFunctionalTest : GhCliAuthFunctionalTestSetup() {
    @Test
    fun `should apply settings plugin with no issues`() {
        val randomTokenValue = "ghp_${System.currentTimeMillis()}"

        val output = project
            .apply { writeSettings() }
            .withArguments("help", "--info")
            .withEnvironment(mapOf(DEFAULT_TOKEN_ENV_KEY to randomTokenValue))
            .build()
            .output

        output
            .shouldContain("Registering GitHub Packages maven repository for organization: $GIVEN_ORG_VALUE")
            .shouldContain("gh.cli.auth.token: $randomTokenValue")
    }

    @Test
    fun `should use default environment variable if present`() {
        val tokenFromDefaultEnv = "ghp_token_from_default_env_123"

        // Do NOT override env name; provider should try DEFAULT_TOKEN_ENV_KEY first
        writeSettings()

        val output = project
            .withArguments("help", "--debug")
            .withEnvironment(mapOf(DEFAULT_TOKEN_ENV_KEY to tokenFromDefaultEnv))
            .build()
            .output

        output
            .shouldContain("Attempting to use GitHub credentials from environment variable: $DEFAULT_TOKEN_ENV_KEY")
            .shouldContain("gh.cli.auth.token: $tokenFromDefaultEnv")
    }

    @Test
    fun `should allow setting a custom environment variable for the token`() {
        val customKey = "CUSTOM_ENV_VAR"
        val customToken = "ghp_exampletoken1234567890"

        val output = project
            .apply {
                // Let the provider pick up the custom env var
                propertiesFile.appendText("\ngh.cli.auth.env.name=$customKey\n")
                writeSettings()
            }
            .withArguments("help", "--debug")
            .withEnvironment(mapOf(customKey to customToken))
            .build()
            .output

        output
            .shouldContain("Attempting to use GitHub credentials from environment variable: $customKey")
            .shouldContain("gh.cli.auth.token: $customToken")
    }

    @Test
    fun `should share token with other settings plugins via gradle extra`() {
        val customKey = "ANOTHER_CUSTOM_ENV"
        val expectedToken = "ghp_token_shared_via_extra"

        val output = project
            .apply {
                propertiesFile.appendText("\ngh.cli.auth.env.name=$customKey\n")
                writeSettings(
                    afterEvalExtra = """
                        // Simulate another settings plugin reading the shared token
                        val shared = gradle.extra.get("gh.cli.auth.token")
                        println("gh.cli.auth.token (read by another settings plugin): ${'$'}shared")
                    """.trimIndent()
                )
            }
            .withArguments("help", "--info")
            .withEnvironment(mapOf(customKey to expectedToken))
            .build()
            .output

        output
            .shouldContain("gh.cli.auth.token: $expectedToken")
            .shouldContain("gh.cli.auth.token (read by another settings plugin): $expectedToken")
    }

    @Test
    fun `should fallback to gh CLI auth when environment variable is not found`() {
        val missingKey = "NON_EXISTENT_ENV_${System.currentTimeMillis()}"
        val missingPropKey = "non.existent.prop.${System.currentTimeMillis()}"

        val output = project
            .apply {
                propertiesFile
                    .appendText(
                        "\n" +
                            "$GH_ENV_KEY_SETTER_PROPERTY=$missingKey\n" +
                            "$GH_PROPERTY_KEY_SETTER_PROPERTY=$missingPropKey\n"
                    )
                writeSettings(
                    afterEvalExtra = """
                        // print maven repo details for verification
                        val repo = dependencyResolutionManagement
                            .repositories
                            .findByName("$GIVEN_ORG_VALUE")
                            .let { it as org.gradle.api.artifacts.repositories.MavenArtifactRepository }
                            
                        println("Maven Repo URL: " + repo.url)
                        val credentials = repo.credentials
                        println("Maven Repo Username: " + credentials.username)
                        println("Maven Repo Password: " + credentials.password)
                    """.trimIndent()
                )
            }
            .withArguments("help", "--debug")
            .build()
            .output

        output
            .shouldContain("Attempting to use GitHub credentials from gh CLI.")
            .shouldContain("Maven Repo URL: https://maven.pkg.github.com/$GIVEN_ORG_VALUE/*")
            .shouldContain("Maven Repo Username: ${GhCliAuthFake.DEFAULT_USER_VALUE}")
            .shouldContain("Maven Repo Password: ${GhCliAuthFake.DEFAULT_TOKEN_VALUE}")
    }

    @Test
    fun `should configure gradle plugin portal repository for pluginManagement`() {
        // PM is missing Gradle Plugin Portal; PM only has Google repository
        writeSettings(
            pmReposBlock = """
                google()
            """.trimIndent()
        )

        val output = project
            .withArguments("help", "--info")
            .build()
            .output

        output
            .shouldContain("Adding Gradle Plugin Portal repository")
            .shouldContain("Registering GitHub Packages maven repository for organization: $GIVEN_ORG_VALUE")
    }

    @Test
    fun `should configure gradle plugin portal repository for dependencyResolutionManagement`() {
        // DRM is missing Gradle Plugin Portal; DRM only has Maven Central repository
        writeSettings(
            drmReposBlock = """
                mavenCentral()
            """.trimIndent()
        )

        val output = project
            .withArguments("help", "--info")
            .build()
            .output

        output
            .shouldContain("Adding Gradle Plugin Portal repository")
            .shouldContain("Registering GitHub Packages maven repository for organization: $GIVEN_ORG_VALUE")
    }

    @Test
    fun `should use token from default gradle property when environment variable is not set`() {
        val expectedToken = "ghp_property_default_${System.currentTimeMillis()}"
        val missingEnvKey = "NON_EXISTENT_ENV_${System.currentTimeMillis()}"

        val output = project
            .apply {
                propertiesFile
                    .apply { appendText("\n") }
                    .appendText(
                        """
                        $GH_ENV_KEY_SETTER_PROPERTY=$missingEnvKey
                        """.trimIndent()
                    )
                writeSettings()
            }
            .withArguments("help", "-P${DEFAULT_TOKEN_PROPERTY_KEY}=$expectedToken", "--debug")
            .build()
            .output

        output
            .shouldContain("Attempting to use GitHub credentials from gradle property: $DEFAULT_TOKEN_PROPERTY_KEY")
            .shouldContain("gh.cli.auth.token: $expectedToken")
    }

    @Test
    fun `should allow setting a custom gradle property key for the token`() {
        val customPropKey = "my.custom.token.prop"
        val expectedToken = "ghp_property_custom_${System.currentTimeMillis()}"
        val missingEnvKey = "NON_EXISTENT_ENV_${System.currentTimeMillis()}"

        val output = project
            .apply {
                propertiesFile
                    .apply { appendText("\n") }
                    .appendText(
                        """
                        $GH_ENV_KEY_SETTER_PROPERTY=$missingEnvKey
                        $GH_PROPERTY_KEY_SETTER_PROPERTY=$customPropKey
                        """.trimIndent()
                    )
                writeSettings()
            }
            .withArguments("help", "-P$customPropKey=$expectedToken", "--debug")
            .build()
            .output

        output
            .shouldContain("Attempting to use GitHub credentials from gradle property: $customPropKey")
            .shouldContain("gh.cli.auth.token: $expectedToken")
    }

    private fun writeSettings(
        pmReposBlock: String? = null,
        drmReposBlock: String? = null,
        afterEvalExtra: String = "",
    ) {
        val pm = pmReposBlock?.let {
            """
            pluginManagement {
                repositories {
                    $it
                }
            }
            """.trimIndent()
        }.orEmpty()

        val drm = drmReposBlock?.let {
            """
            @Suppress("UnstableApiUsage")
            dependencyResolutionManagement {
                repositories {
                    $it
                }
            }
            """.trimIndent()
        }.orEmpty()

        settingsFile.writeText(
            """
            // Optional user-provided initial repositories (to test addTrustedRepositoriesIfMissing behavior)
            $pm
            $drm
            
            // Apply the gh-cli-auth settings plugin
            plugins {
                id("io.github.adelinosousa.gradle.plugins.settings.gh-cli-auth")
            }

            // Helper to verify final state *after* settings have been evaluated
            fun afterSettingsEvaluate(action: Settings.() -> Unit) {
                gradle.settingsEvaluated { action(this) }
            }

            // Verify that the plugin shared the token via gradle.extra and allow custom assertions
            afterSettingsEvaluate {
                val tokenName = "gh.cli.auth.token"
                val ghToken = gradle.extra.get(tokenName)
                println("${'$'}tokenName: ${'$'}ghToken")
                $afterEvalExtra
            }
            """.trimIndent()
        )
    }
}
