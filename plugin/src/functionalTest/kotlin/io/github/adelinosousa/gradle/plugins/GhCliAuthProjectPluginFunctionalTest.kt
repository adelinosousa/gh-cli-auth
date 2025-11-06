package io.github.adelinosousa.gradle.plugins

import io.github.adelinosousa.gradle.plugins.GhCliAuthBase.Companion.DEFAULT_TOKEN_ENV_KEY
import io.github.adelinosousa.gradle.plugins.GhCliAuthBase.Companion.DEFAULT_TOKEN_USERNAME
import io.github.adelinosousa.gradle.plugins.GhCliAuthBase.Companion.GH_CLI_EXTENSION_NAME
import io.github.adelinosousa.gradle.plugins.GhCliAuthBase.Companion.GH_ORG_SETTER_PROPERTY
import io.github.adelinosousa.gradle.plugins.support.GhCliAuthFake
import io.github.adelinosousa.gradle.plugins.support.GhCliAuthFunctionalTestSetup
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import org.junit.jupiter.api.BeforeEach

class GhCliAuthProjectPluginFunctionalTest : GhCliAuthFunctionalTestSetup() {
    @BeforeEach
    fun setup() {
        buildFile.appendText(
            """
            plugins { id("io.github.adelinosousa.gradle.plugins.project.gh-cli-auth") }
            
            // print ghCliAuth extension token value for verification
            tasks.register("printGhCliAuthToken") {
                doLast {
                    println("GhCliAuth Extension Token: " + project.extensions.getByName("$GH_CLI_EXTENSION_NAME").let { ext ->
                        ext as io.github.adelinosousa.gradle.extensions.GhCliAuthExtension
                        ext.token.get()
                    })
                }
            }
            
            // print the org user name and token for maven repo for verification
            tasks.register("printGhPackagesRepoConfig") {
                doLast {
                    val repo = project.repositories.findByName("$GIVEN_ORG_VALUE")
                    if (repo is MavenArtifactRepository) {
                        println("Maven Repo URL: " + repo.url)
                        val credentials = repo.credentials
                        println("Maven Repo Username: " + credentials.username)
                        println("Maven Repo Password: " + credentials.password)
                    } else {
                        println("Repository '$GIVEN_ORG_VALUE GitHub Packages' not found...")
                    }
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun `should apply plugin with no issues`() {
        val randomTokenValue = "ghp_${System.currentTimeMillis()}"

        project
            .withArguments("printGhPackagesRepoConfig", "--info")
            .withEnvironment(mapOf(DEFAULT_TOKEN_ENV_KEY to randomTokenValue))
            .build()
            .output
            .shouldContain("Registering GitHub Packages maven repository for organization: $GIVEN_ORG_VALUE")
            .shouldContain("Maven Repo URL: https://maven.pkg.github.com/$GIVEN_ORG_VALUE/*")
            .shouldContain("Maven Repo Username: $DEFAULT_TOKEN_USERNAME")
            .shouldContain("Maven Repo Password: $randomTokenValue")
    }

    @Test
    fun `should allow setting a custom environment variable for the token`() {
        val customEnvKey = "CUSTOM_ENV_KEY"
        val customFakeTokenValue = "ghp_fake_token_value"

        propertiesFile
            .appendText("\ngh.cli.auth.env.name=$customEnvKey")

        project
            .withArguments("printGhCliAuthToken", "--debug")
            .withEnvironment(mapOf(customEnvKey to customFakeTokenValue))
            .build()
            .output
            .shouldContain("Attempting to use GitHub credentials from environment variable: $customEnvKey")
            .shouldContain("GhCliAuth Extension Token: $customFakeTokenValue")
    }

    @Test
    fun `should fallback to gh CLI auth when environment variable is not found`() {
        val badEnvKey = "NON_EXISTENT_ENV_KEY-${System.currentTimeMillis()}"

        propertiesFile
            .appendText("\ngh.cli.auth.env.name=$badEnvKey")

        project
            .withArguments("printGhCliAuthToken", "--debug")
            .build()
            .output
            .shouldContain("Falling back to gh CLI for GitHub credentials.")
            .shouldContain("GhCliAuth Extension Token: ${GhCliAuthFake.DEFAULT_TOKEN_VALUE}")
    }

    @Test
    fun `should fail when org is not set in gradle properties`() {
        project
            .apply {
                // remove org property from gradle.properties
                propertiesFile.writeText(
                    propertiesFile
                        .readText()
                        .lines()
                        .filterNot { it.startsWith(GH_ORG_SETTER_PROPERTY) }
                        .joinToString("\n")
                )
            }
            .buildAndFail()
            .output
            .shouldContain("Please set '$GH_ORG_SETTER_PROPERTY' in gradle.properties.")
    }
}
