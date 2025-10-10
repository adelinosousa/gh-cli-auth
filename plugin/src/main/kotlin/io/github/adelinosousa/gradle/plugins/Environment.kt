package io.github.adelinosousa.gradle.plugins

internal object Environment {
    internal fun getEnv(name: String): String? {
        return System.getenv(name)
    }

    internal fun getEnvCredentials(gitEnvTokenName: String) : RepositoryCredentials? {
        val token = getEnv(gitEnvTokenName)
        if (!token.isNullOrEmpty()) {
            return RepositoryCredentials("", token)
        }
        return null
    }
}