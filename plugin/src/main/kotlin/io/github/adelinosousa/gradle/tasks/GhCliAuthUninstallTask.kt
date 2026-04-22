package io.github.adelinosousa.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.io.File

@UntrackedTask(because = "Deletes from Gradle user home which is outside the project directory")
public abstract class GhCliAuthUninstallTask : DefaultTask() {

    internal companion object {
        const val TASK_NAME: String = "ghCliAuthUninstall"
    }

    @get:Input
    public abstract val gradleUserHomeDir: Property<String>

    @TaskAction
    public fun uninstall() {
        val gradleUserHome = File(gradleUserHomeDir.get())
        val initScript = gradleUserHome.resolve("init.d/${GhCliAuthInstallTask.INIT_SCRIPT_NAME}")
        val deleted = initScript.delete()

        if (deleted) {
            logger.lifecycle("Removed init script: ${initScript.absolutePath}")
        } else {
            logger.lifecycle("No init script found at: ${initScript.absolutePath}")
        }
    }
}
