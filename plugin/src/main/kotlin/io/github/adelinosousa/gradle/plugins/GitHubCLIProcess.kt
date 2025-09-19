package io.github.adelinosousa.gradle.plugins

import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

abstract class GitHubCLIProcess : ValueSource<String, ValueSourceParameters.None> {

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String? {
        return try {
            val ghPath = findGhPath()
            val outputStream = ByteArrayOutputStream()
            val process = execOperations.exec {
                commandLine(ghPath, "auth", "status", "--show-token")
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

    private fun findGhPath(): String {
        // Path fix for macOS
        if ("mac" in System.getProperty("os.name").lowercase()) {
            val homebrewPaths = listOf("/opt/homebrew/bin/gh", "/usr/local/bin/gh")
            return homebrewPaths.firstOrNull { File(it).exists() } ?: "gh"
        }

        return "gh"
    }
}

