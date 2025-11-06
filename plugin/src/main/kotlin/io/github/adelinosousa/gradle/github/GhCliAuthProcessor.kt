package io.github.adelinosousa.gradle.github

import io.github.adelinosousa.gradle.plugins.GhCliAuthBase.Companion.GH_CLI_EXTENSION_NAME
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations

internal abstract class GhCliAuthProcessor : ValueSource<String, ValueSourceParameters.None> {
    companion object {
        private val logger = Logging.getLogger(GH_CLI_EXTENSION_NAME)

        /**
         * System property that can be used to override the path to the `gh` binary.
         * This skips the default detection logic in place which can be incorrect in some environments.
         */
        internal const val GH_CLI_BINARY_PATH: String = "gh.cli.binary.path"

        @JvmStatic
        internal fun create(factory: ProviderFactory): Provider<String> =
            factory.of(GhCliAuthProcessor::class.java) {}
    }

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
        val customGhBinaryPath = System.getProperty(GH_CLI_BINARY_PATH)

        if (customGhBinaryPath != null) {
            logger.debug("Using custom gh binary path from system property: $customGhBinaryPath")
            return customGhBinaryPath
        } else {
            val osName = System.getProperty("os.name").lowercase()
            logger.debug("Detecting gh binary for OS: $osName")
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
}