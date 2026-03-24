package io.github.adelinosousa.gradle.github

import io.github.adelinosousa.gradle.plugins.GhCliAuthBase.Companion.GH_CLI_EXTENSION_NAME
import org.gradle.api.logging.Logging

internal object GhCliAuthParser {
    private val logger = Logging.getLogger(GH_CLI_EXTENSION_NAME)
    private val REQUIRED_RESOURCES: Set<String> = setOf("packages", "org")
    private val REQUIRED_SCOPES: Set<String> = setOf("read", "write", "admin")

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

        val missingResources = REQUIRED_RESOURCES.filterNot { isScopeSatisfied(it, scopes) }

        check(missingResources.isEmpty()) {
            val expected = missingResources.flatMap { resource -> REQUIRED_SCOPES.map { scope -> "$scope:$resource" } }
            "GitHub CLI token is missing required scopes. Required: $expected, Found: $scopes"
        }
    }

    private fun isScopeSatisfied(resource: String, availableScopes: List<String>): Boolean =
        availableScopes.any { scope ->
            val parts = scope.split(":", limit = 2)
            parts.size == 2 && parts[1] == resource && parts[0] in REQUIRED_SCOPES
        }
}