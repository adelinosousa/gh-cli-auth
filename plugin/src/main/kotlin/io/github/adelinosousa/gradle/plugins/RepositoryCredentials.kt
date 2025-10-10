package io.github.adelinosousa.gradle.plugins

internal class RepositoryCredentials(
    internal val username: String?,
    internal val token: String?
) {
    internal fun isValid(): Boolean {
        return username != null && !token.isNullOrEmpty()
    }
}