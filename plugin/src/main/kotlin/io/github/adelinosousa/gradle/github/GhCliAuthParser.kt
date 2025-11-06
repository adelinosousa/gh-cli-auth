package io.github.adelinosousa.gradle.github

import io.github.adelinosousa.gradle.plugins.GhCliAuthBase.Companion.GH_CLI_EXTENSION_NAME
import org.gradle.api.logging.Logging

internal object GhCliAuthParser {
    private val logger = Logging.getLogger(GH_CLI_EXTENSION_NAME)
    private val REQUIRED_SCOPES: Set<String> = setOf("read:packages", "read:org")

    internal fun parse(output: String): GhCredentials = this
        .validate(output)
        .runCatching {
            val user = output.lines()
                .firstOrNull { it.contains("Logged in to github.com account") }
                ?.substringAfterLast("account")
                ?.replace("(keyring)", "")
                ?.trim()
                .let { requireNotNull(it) { "'gh' CLI output: failed to extract username" } }

            val token = output.lines()
                .firstOrNull { it.contains("Token:") }
                ?.substringAfter(":")
                ?.trim()
                .let { requireNotNull(it) { "'gh' CLI output: failed to extract token" } }

            GhCredentials(user, token)
        }.onFailure { e ->
            logger.error("Failed to get credentials from 'gh' CLI: ${e.message}")
        }.getOrElse {
            error("'gh' CLI is authenticated but failed to extract user or token")
        }

    private fun validate(output: String) {
        val scopes = output
            .lines()
            .firstOrNull { it.contains("Token scopes:") }
            ?.substringAfter(":")
            ?.trim()
            ?.split(",")
            ?.map { it.replace("'", "").trim() }
            ?: emptyList()

        check(scopes.containsAll(REQUIRED_SCOPES)) {
            "GitHub CLI token is missing required scopes. Required: $REQUIRED_SCOPES, Found: $scopes"
        }
    }
}