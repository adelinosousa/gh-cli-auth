package io.github.adelinosousa.gradle.plugins

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property

internal class GhCliAuthProjectPlugin : Plugin<Project> {
    private companion object {
        private val logger = Logging.getLogger(GhCliAuthSettingsPlugin::class.java)
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create("ghCliAuth", GhCliAuthExtension::class.java)

        val githubOrg = getGradleProperty(project, Config.GITHUB_ORG)
        val gitEnvTokenName = getGradleProperty(project, Config.ENV_PROPERTY_NAME) ?: "GITHUB_TOKEN"

        if (githubOrg.isNullOrEmpty()) {
            throw IllegalStateException("GitHub organization not specified. Please set the '${Config.GITHUB_ORG}' in your gradle.properties file.")
        }

        val repoCredentials = Environment.getEnvCredentials(gitEnvTokenName) ?: getGhCliCredentials(project)
        if (repoCredentials.isValid()) {
            logger.info("Registering Maven GitHub repository for organization: $githubOrg")
            // Set the extension token to share with other tasks
            extension.token.set(repoCredentials.token)
            project.repositories.maven {
                name = "GitHubPackages"
                url = project.uri("https://maven.pkg.github.com/$githubOrg/*")
                credentials {
                    this.username = repoCredentials.username
                    this.password = repoCredentials.token
                }
            }
        } else {
            throw IllegalStateException("Token not found in environment variable '${gitEnvTokenName}' or 'gh' CLI. Unable to configure GitHub Packages repository.")
        }
    }

    private fun getGhCliCredentials(project: Project): RepositoryCredentials {
        val authStatusProvider = project.providers.of(GitHubCLIProcess::class.java) {}
        val output = GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes(authStatusProvider)
        return GhCliAuth.getGitHubCredentials(output.get())
    }

    private fun getGradleProperty(project: Project, propertyName: String): String? {
        return project.providers.gradleProperty(propertyName).orNull
    }
}

public interface GhCliAuthExtension {
    public val token: Property<String?>
}
