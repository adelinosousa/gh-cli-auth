package io.github.adelinosousa.gradle.plugins

import io.github.adelinosousa.gradle.extensions.GhCliAuthExtension
import io.github.adelinosousa.gradle.tasks.GhCliAuthInstallTask
import io.github.adelinosousa.gradle.tasks.GhCliAuthUninstallTask
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

        registerInstallerTasks(project)
    }

    private fun registerInstallerTasks(project: Project) {
        val gradleUserHome = project.providers.provider { project.gradle.gradleUserHomeDir.absolutePath }
        val githubOrg = project.providers.gradleProperty(GH_ORG_SETTER_PROPERTY)

        project.tasks.register(GhCliAuthInstallTask.TASK_NAME, GhCliAuthInstallTask::class.java) {
            group = GhCliAuthInstallTask.TASK_GROUP
            description = "Installs a Gradle init script for GitHub Packages authentication via gh CLI."
            this.githubOrg.set(githubOrg)
            gradleUserHomeDir.convention(gradleUserHome)
        }

        project.tasks.register(GhCliAuthUninstallTask.TASK_NAME, GhCliAuthUninstallTask::class.java) {
            group = GhCliAuthInstallTask.TASK_GROUP
            description = "Removes the Gradle init script for GitHub Packages authentication."
            gradleUserHomeDir.convention(gradleUserHome)
        }
    }
}
