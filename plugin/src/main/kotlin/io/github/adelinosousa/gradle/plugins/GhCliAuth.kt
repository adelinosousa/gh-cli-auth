package io.github.adelinosousa.gradle.plugins

import org.gradle.api.GradleException
import java.io.ByteArrayOutputStream

object GhCliAuth {
    const val DEBUG: Boolean = true
    const val GITHUB_ORG: String = "gh.cli.auth.github.org"
    const val ENV_PROPERTY_NAME: String = "gh.cli.auth.env.name"
    val requiredScopes: Set<String> = setOf("read:packages", "repo", "read:org")

    fun log(message: String) {
        if (DEBUG) {
            println(message)
        }
    }

    private fun checkGhCliInstalled() {
        try {
            ProcessBuilder("gh", "--version").start().waitFor() == 0
        } catch (_: Exception) {
            throw GradleException("GitHub CLI is not installed or not found in PATH. Please install it before using this plugin.")
        }
    }

    fun checkGhCliAuthenticatedWithCorrectScopes(): String {
        checkGhCliInstalled()
        try {
            val process = ProcessBuilder("gh", "auth", "status", "--show-token").start()
            val outputStream = ByteArrayOutputStream()
            process.inputStream.copyTo(outputStream)
            val output = outputStream.toString().trim()

            log("Output from 'gh auth status': $output")

            if (process.waitFor() == 0 && output.contains("Token:")) {
                val scopesLine = output.lines().firstOrNull { it.contains("Token scopes:") }
                val scopes = scopesLine?.substringAfter(":")?.trim()?.split(",")?.map { it.replace("'", "").trim() }
                    ?: emptyList()

                log("Found GitHub credentials with scopes: $scopes")

                if (scopes.containsAll(requiredScopes)) {
                    log("GitHub CLI is authenticated with the required scopes: $requiredScopes")
                    return output
                }
            }

            throw GradleException("GitHub CLI is not authenticated or does not have the required scopes $requiredScopes")
        } catch (e: Exception) {
            throw GradleException("Failed to check GitHub CLI authentication status - ${e.message}")
        }
    }

    fun getGitHubCredentials(output: String, gitEnvToken: String?): Pair<String?, String?> {
        try {
            val userLine = output.lines().firstOrNull { it.contains("Logged in to github.com account") }
            val tokenLine = output.lines().firstOrNull { it.contains("Token:") }

            var user = userLine?.substringAfterLast("account")?.replace("(keyring)", "")?.trim()
            var token = tokenLine?.substringAfter(":")?.trim()

            log("Found GitHub credentials using 'gh' CLI: user='$user', token='${token?.take(4)}...'")

            if (user != null && token != null) {
                log("Found GitHub credentials using 'gh' CLI for user '$user'")
                return Pair(user, token)
            }

            if (!gitEnvToken.isNullOrEmpty()) {
                log("Falling back to environment variable for credentials.")
                user = "" // Default to empty username
                token = System.getenv(gitEnvToken)
                return Pair(user, token)
            }
        } catch (e: Exception) {
            log("Failed to get credentials from 'gh' CLI: ${e.message}")
        }

        throw GradleException("'gh' CLI is authenticated but failed to extract user or token")
    }
}