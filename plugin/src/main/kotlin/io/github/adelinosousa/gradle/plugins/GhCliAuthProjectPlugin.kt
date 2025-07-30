package io.github.adelinosousa.gradle.plugins

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.provider.Property

@Suppress("unused")
class GhCliAuthProjectPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("ghCliAuth", GhCliAuthExtension::class.java)

        val githubOrg = getGradleProperty(project, GhCliAuth.GITHUB_ORG)
        val gitEnvToken = getGradleProperty(project, GhCliAuth.ENV_PROPERTY_NAME)

        project.logger.info("Applying GitHubAuthPlugin to project")

        val output = GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes()

        val (username, token) = GhCliAuth.getGitHubCredentials(output, gitEnvToken)
        if (!username.isNullOrEmpty() && !token.isNullOrEmpty() && !githubOrg.isNullOrEmpty()) {
            GhCliAuth.log("Configuring GitHub Packages maven repository for '$githubOrg'")
            extension.token.set(token)
            project.repositories.maven {
                name = "GitHubPackages"
                url = project.uri("https://maven.pkg.github.com/$githubOrg/*")
                credentials {
                    this.username = username
                    this.password = token
                }
            }
        } else {
            throw IllegalStateException("GitHub token not found. Unable to configure GitHub Packages repository.")
        }
    }

    private fun getGradleProperty(project: Project, propertyName: String): String? {
        return project.providers.gradleProperty(propertyName).let {
            if (it.isPresent) it.get() else null
        }
    }
}

interface GhCliAuthExtension {
    val token: Property<String?>
}
