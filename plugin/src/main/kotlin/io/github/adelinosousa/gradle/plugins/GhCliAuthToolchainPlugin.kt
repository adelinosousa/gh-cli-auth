package io.github.adelinosousa.gradle.plugins

import io.github.adelinosousa.gradle.tasks.GhCliAuthInstallTask
import io.github.adelinosousa.gradle.tasks.GhCliAuthUninstallTask
import org.gradle.api.Plugin
import org.gradle.api.Project

public class GhCliAuthToolchainPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val providers = project.providers
        val gradleUserHome = providers.provider { project.gradle.gradleUserHomeDir.absolutePath }
        val githubOrg = providers.gradleProperty(GhCliAuthBase.GH_ORG_SETTER_PROPERTY)

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
