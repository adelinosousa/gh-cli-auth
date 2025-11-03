package io.github.adelinosousa.gradle.internal

import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations

internal abstract class GhCLIProcess : ValueSource<String, ValueSourceParameters.None> {

    @get:Inject
    internal abstract val execOperations: ExecOperations

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
                throw IllegalStateException("Failed to process GitHub CLI command.")
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to authenticate: ${e.message}. GitHub CLI is probably not installed or not found in PATH. Please install it before using this plugin, more information visit: https://gh-cli-auth.digibit.uk.", e)
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