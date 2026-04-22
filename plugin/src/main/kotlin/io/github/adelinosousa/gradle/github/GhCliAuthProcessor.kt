package io.github.adelinosousa.gradle.github

import io.github.adelinosousa.gradle.plugins.GhCliAuthBase.Companion.GH_CLI_EXTENSION_NAME
import java.io.ByteArrayOutputStream
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
                commandLine(GhBinaryResolver.resolve(), "auth", "status", "--show-token")
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
}