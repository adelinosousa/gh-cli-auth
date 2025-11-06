package io.github.adelinosousa.gradle.plugins

import io.github.adelinosousa.gradle.extensions.GhCliAuthExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

public class GhCliAuthProjectPlugin : GhCliAuthBase(), Plugin<Project> {
    override fun apply(project: Project) {
        val provider = project.providers

        val extension = (project
            .extensions.findByType(GhCliAuthExtension::class.java)
            ?: project.extensions.create(GH_CLI_EXTENSION_NAME, GhCliAuthExtension::class.java))

        extension
            .token
            .set(provider.credentials.token)

        project
            .repositories
            .addUserConfiguredOrgGhPackagesRepository(provider)
    }
}
