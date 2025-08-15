package io.github.adelinosousa.gradle.plugins

import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

abstract class GitHubCLIProcess : ValueSource<String, ValueSourceParameters.None> {

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String? {
        return try {
            val outputStream = ByteArrayOutputStream()
            val process = execOperations.exec {
                commandLine("gh", "auth", "status", "--show-token")
                standardOutput = outputStream
                isIgnoreExitValue = true
            }

            if (process.exitValue == 0) {
                outputStream.toString().trim()
            } else {
                val checkInstall = execOperations.exec {
                    commandLine("gh", "--version")
                    isIgnoreExitValue = true
                }

                if (checkInstall.exitValue != 0) {
                    throw IllegalStateException("GitHub CLI is not installed or not found in PATH. Please install it before using this plugin.")
                }

                throw IllegalStateException("Failed to execute 'gh auth status'")
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to authenticate: ${e.message}", e)
        }
    }
}

