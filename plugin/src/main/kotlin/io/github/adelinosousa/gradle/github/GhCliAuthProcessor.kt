package io.github.adelinosousa.gradle.github

import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations

internal abstract class GhCliAuthProcessor : ValueSource<String, ValueSourceParameters.None> {

    @get:Inject
    internal abstract val execOperations: ExecOperations

    override fun obtain(): String? = runCatching {
        val outputStream = ByteArrayOutputStream()

        execOperations
            .exec {
                commandLine(dynamicGhBin(), "auth", "status", "--show-token")
                standardOutput = outputStream
                isIgnoreExitValue = true
            }
            .assertNormalExitValue()

        outputStream.toString().trim()
    }.getOrElse { e ->
        throw IllegalStateException(
            "Failed to authenticate: ${e.message}. " +
                "GitHub CLI is probably not installed or not found in PATH. " +
                "Please install it before using this plugin, more information visit: https://gh-cli-auth.digibit.uk.",
            e
        )
    }

    private fun dynamicGhBin(): String {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            "mac" in osName -> {
                // NOTE: In theory, this shouldn't be needed if the user has set up their PATH correctly.
                listOf(
                    "/opt/homebrew/bin/gh",  // Apple Silicon
                    "/usr/local/bin/gh",     // Intel
                    "/usr/bin/gh"            // System install
                ).firstOrNull { File(it).exists() } ?: "gh"
            }
            "windows" in osName -> "gh.exe"
            else -> "gh"
        }
    }
}