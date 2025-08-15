package io.github.adelinosousa.gradle.plugins

import org.gradle.api.GradleException
import org.gradle.api.provider.Provider

object GhCliAuth {
    val requiredScopes: Set<String> = setOf("read:packages", "repo", "read:org")

    fun checkGhCliAuthenticatedWithCorrectScopes(authStatusProvider: Provider<String>): Provider<String> {
        return authStatusProvider.map { output ->
            if (output.contains("Token:")) {
                val scopesLine = output.lines().firstOrNull { it.contains("Token scopes:") }
                val scopes = scopesLine?.substringAfter(":")?.trim()?.split(",")?.map { it.replace("'", "").trim() }
                    ?: emptyList()

                if (scopes.containsAll(requiredScopes)) {
                    return@map output
                }
            }
            throw GradleException("GitHub CLI is not authenticated or does not have the required scopes $requiredScopes")
        }
    }

    fun getGitHubCredentials(output: String): RepositoryCredentials {
        try {
            val userLine = output.lines().firstOrNull { it.contains("Logged in to github.com account") }
            val tokenLine = output.lines().firstOrNull { it.contains("Token:") }

            val user = userLine?.substringAfterLast("account")?.replace("(keyring)", "")?.trim()
            val token = tokenLine?.substringAfter(":")?.trim()

            if (user != null && token != null) {
                return RepositoryCredentials(user, token)
            }
        } catch (e: Exception) {
            println("Failed to get credentials from 'gh' CLI: ${e.message}")
        }

        throw GradleException("'gh' CLI is authenticated but failed to extract user or token")
    }
}