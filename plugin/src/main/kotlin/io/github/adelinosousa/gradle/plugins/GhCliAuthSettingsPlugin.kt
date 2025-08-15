package io.github.adelinosousa.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.initialization.Settings
import java.net.URI

@Suppress("unused")
class GhCliAuthSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        println("Applying GitHubAuthPlugin to settings")

        val githubOrg = getGradleProperty(settings, Config.GITHUB_ORG)
        val gitEnvTokenName = getGradleProperty(settings, Config.ENV_PROPERTY_NAME) ?: "GITHUB_TOKEN"

        if (githubOrg.isNullOrEmpty()) {
            throw IllegalStateException("GitHub organization not specified. Please set the '${Config.GITHUB_ORG}' in your gradle.properties file.")
        }

        val repoCredentials = Environment.getEnvCredentials(gitEnvTokenName) ?: getGhCliCredentials(settings)
        if (repoCredentials.isValid()) {
            val githubMavenRepository = { repo: MavenArtifactRepository ->
                repo.name = "GitHubPackages"
                repo.url = URI("https://maven.pkg.github.com/$githubOrg/*")
                repo.credentials {
                    this.username = repoCredentials.username
                    this.password = repoCredentials.token
                }
            }

            settings.pluginManagement.repositories.maven(githubMavenRepository)
            settings.dependencyResolutionManagement.repositories.maven(githubMavenRepository)
        } else {
            throw IllegalStateException("Token not found in environment variable '${gitEnvTokenName}' or 'gh' CLI. Unable to configure GitHub Packages repository.")
        }
    }

    private fun getGhCliCredentials(settings: Settings): RepositoryCredentials {
        println("No GitHub credentials found in environment variables. Attempting to use 'gh' CLI.")
        val authStatusProvider = settings.providers.of(GitHubCLIProcess::class.java) {}
        val output = GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes(authStatusProvider)
        return GhCliAuth.getGitHubCredentials(output.get())
    }

    private fun getGradleProperty(settings: Settings, propertyName: String): String? {
        return settings.providers.gradleProperty(propertyName).orNull
    }
}