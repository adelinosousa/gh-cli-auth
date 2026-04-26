package io.github.adelinosousa.gradle.tasks

import io.github.adelinosousa.gradle.github.GhCliAuthParser
import io.github.adelinosousa.gradle.github.GhCliAuthProcessor
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.io.File
import javax.inject.Inject

@UntrackedTask(because = "Writes to Gradle user home which is outside the project directory")
public abstract class GhCliAuthInstallTask : DefaultTask() {

    internal companion object {
        const val TASK_NAME: String = "ghCliAuthInstall"
        const val TASK_GROUP: String = "gh-cli-auth"
    }

    @get:Input
    public abstract val githubOrg: Property<String>

    @get:Input
    public abstract val gradleUserHomeDir: Property<String>

    @get:Inject
    internal abstract val providers: ProviderFactory

    @TaskAction
    public fun install() {
        GhCliAuthProcessor
            .create(providers)
            .get()
            .run(GhCliAuthParser::parse)

        GhCliAuthInstaller.installIfNeeded(
            gradleUserHome = File(gradleUserHomeDir.get()),
            githubOrg = githubOrg.get(),
        )
    }
}
