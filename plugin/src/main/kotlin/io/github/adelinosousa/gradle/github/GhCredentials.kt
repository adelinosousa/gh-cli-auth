package io.github.adelinosousa.gradle.github

public data class GhCredentials(
    internal val username: String,
    internal val token: String,
) {
    override fun toString(): String = "GhCredentials(username=$username, token=***)"
}