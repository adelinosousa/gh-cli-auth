package io.github.adelinosousa.gradle.plugins

class RepositoryCredentials(
    val username: String?,
    val token: String?
) {
    fun isValid(): Boolean {
        return username != null && !token.isNullOrEmpty()
    }
}