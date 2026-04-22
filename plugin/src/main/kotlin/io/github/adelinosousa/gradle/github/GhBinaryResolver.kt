package io.github.adelinosousa.gradle.github

import io.github.adelinosousa.gradle.plugins.GhCliAuthBase.Companion.GH_CLI_EXTENSION_NAME
import java.io.File
import org.gradle.api.logging.Logging

internal object GhBinaryResolver {
    private val logger = Logging.getLogger(GH_CLI_EXTENSION_NAME)

    internal const val GH_CLI_BINARY_PATH: String = "gh.cli.binary.path"

    internal fun resolve(): String {
        val customGhBinaryPath = System.getProperty(GH_CLI_BINARY_PATH)

        if (customGhBinaryPath != null) {
            val binaryFile = File(customGhBinaryPath)
            require(binaryFile.exists()) { "Custom gh binary path does not exist: $customGhBinaryPath" }
            val binaryName = binaryFile.nameWithoutExtension.lowercase()
            if (binaryName != "gh") {
                logger.warn("Custom gh binary path does not appear to be a 'gh' binary: $customGhBinaryPath")
            }
            logger.debug("Using custom gh binary path from system property: $customGhBinaryPath")
            return customGhBinaryPath
        } else {
            val osName = System.getProperty("os.name").lowercase()
            logger.debug("Detecting gh binary for OS: $osName")
            return when {
                "mac" in osName -> {
                    listOf(
                        "/opt/homebrew/bin/gh",
                        "/usr/local/bin/gh",
                        "/usr/bin/gh"
                    ).firstOrNull { File(it).exists() } ?: "gh"
                }
                "windows" in osName -> "gh.exe"
                else -> "gh"
            }
        }
    }
}
