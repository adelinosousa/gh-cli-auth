package io.github.adelinosousa.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging
import java.net.URI

internal class GhCliAuthSettingsPlugin : Plugin<Settings> {
    private companion object {
        private val logger = Logging.getLogger(GhCliAuthSettingsPlugin::class.java)
    }

    override fun apply(settings: Settings) {
        val githubOrg = getGradleProperty(settings, Config.GITHUB_ORG)
        val gitEnvTokenName = getGradleProperty(settings, Config.ENV_PROPERTY_NAME) ?: "GITHUB_TOKEN"

        if (githubOrg.isNullOrEmpty()) {
            throw IllegalStateException("GitHub organization not specified. Please set the '${Config.GITHUB_ORG}' in your gradle.properties file.")
        }

        val repoCredentials = Environment.getEnvCredentials(gitEnvTokenName) ?: getGhCliCredentials(settings)
        if (repoCredentials.isValid()) {
            settings.pluginManagement.repositories.addRepositoriesWithDefaults(githubOrg, repoCredentials)
            settings.dependencyResolutionManagement.repositories.addRepositoriesWithDefaults(githubOrg, repoCredentials)
        } else {
            throw IllegalStateException("Token not found in environment variable '${gitEnvTokenName}' or 'gh' CLI. Unable to configure GitHub Packages repository.")
        }
    }

    private fun getGhCliCredentials(settings: Settings): RepositoryCredentials {
        val authStatusProvider = settings.providers.of(GitHubCLIProcess::class.java) {}
        val output = GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes(authStatusProvider)
        return GhCliAuth.getGitHubCredentials(output.get())
    }

    private fun getGradleProperty(settings: Settings, propertyName: String): String? {
        return settings.providers.gradleProperty(propertyName).orNull
    }

    private fun RepositoryHandler.addRepositoriesWithDefaults(githubOrg: String, repoCredentials: RepositoryCredentials) {
        if (this.findByName("MavenRepo") == null) {
            logger.info("Adding Maven Central repository")
            this.mavenCentral()
        }

        if (this.findByName("Google") == null) {
            logger.info("Adding Google repository")
            this.google()
        }

        if (this.findByName("Gradle Central Plugin Repository") == null) {
            logger.info("Adding Gradle Plugin Portal repository")
            this.gradlePluginPortal()
        }

        logger.info("Registering Maven GitHub repository for organization: $githubOrg")
        this.maven {
            name = "GitHubPackages"
            url = URI("https://maven.pkg.github.com/$githubOrg/*")
            credentials {
                this.username = repoCredentials.username
                this.password = repoCredentials.token
            }
        }
    }
}