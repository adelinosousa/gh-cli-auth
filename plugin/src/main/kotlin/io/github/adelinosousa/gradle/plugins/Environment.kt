package io.github.adelinosousa.gradle.plugins

object Environment {
    fun getEnv(name: String): String? {
        return System.getenv(name)
    }

    fun getEnvCredentials(gitEnvTokenName: String) : RepositoryCredentials? {
        val token = getEnv(gitEnvTokenName)
        if (!token.isNullOrEmpty()) {
            return RepositoryCredentials("", token)
        }
        return null
    }
}