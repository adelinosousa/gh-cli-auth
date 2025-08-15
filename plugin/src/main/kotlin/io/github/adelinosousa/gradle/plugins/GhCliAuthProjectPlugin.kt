package io.github.adelinosousa.gradle.plugins

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.provider.Property

@Suppress("unused")
class GhCliAuthProjectPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("ghCliAuth", GhCliAuthExtension::class.java)

        project.logger.info("Applying GitHubAuthPlugin to project")

        val githubOrg = getGradleProperty(project, Config.GITHUB_ORG)
        val gitEnvTokenName = getGradleProperty(project, Config.ENV_PROPERTY_NAME) ?: "GITHUB_TOKEN"

        if (githubOrg.isNullOrEmpty()) {
            throw IllegalStateException("GitHub organization not specified. Please set the '${Config.GITHUB_ORG}' in your gradle.properties file.")
        }

        val repoCredentials = Environment.getEnvCredentials(gitEnvTokenName) ?: getGhCliCredentials(project)
        if (repoCredentials.isValid()) {
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
        println("No GitHub credentials found in environment variables. Attempting to use 'gh' CLI.")
        val authStatusProvider = project.providers.of(GitHubCLIProcess::class.java) {}
        val output = GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes(authStatusProvider)
        return GhCliAuth.getGitHubCredentials(output.get())
    }

    private fun getGradleProperty(project: Project, propertyName: String): String? {
        return project.providers.gradleProperty(propertyName).orNull
    }
}

interface GhCliAuthExtension {
    val token: Property<String?>
}
