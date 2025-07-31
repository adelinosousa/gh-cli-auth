package io.github.adelinosousa.gradle.plugins

import org.gradle.api.GradleException

object GhCliAuth {
    val requiredScopes: Set<String> = setOf("read:packages", "repo", "read:org")
    var ghCLIProcess: GitHubCLIProcessProvider = GitHubCLIProcess()

    private fun checkGhCliInstalled() {
        try {
            ghCLIProcess.isGhCliInstalled()
        } catch (_: Exception) {
            throw GradleException("GitHub CLI is not installed or not found in PATH. Please install it before using this plugin.")
        }
    }

    fun checkGhCliAuthenticatedWithCorrectScopes(): String {
        try {
            checkGhCliInstalled()

            val output = ghCLIProcess.getGhCliAuthStatus()
            if (output.contains("Token:")) {
                val scopesLine = output.lines().firstOrNull { it.contains("Token scopes:") }
                val scopes = scopesLine?.substringAfter(":")?.trim()?.split(",")?.map { it.replace("'", "").trim() }
                    ?: emptyList()

                if (scopes.containsAll(requiredScopes)) {
                    return output
                }
            }

            throw GradleException("GitHub CLI is not authenticated or does not have the required scopes $requiredScopes")
        } catch (e: Exception) {
            throw GradleException("Failed to authenticate: ${e.message}")
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