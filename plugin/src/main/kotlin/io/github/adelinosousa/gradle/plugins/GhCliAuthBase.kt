package io.github.adelinosousa.gradle.plugins

import io.github.adelinosousa.gradle.github.GhCliAuthProcessor
import io.github.adelinosousa.gradle.github.GhCliAuthParser
import io.github.adelinosousa.gradle.github.GhCredentials
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ProviderFactory
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import org.gradle.api.logging.Logger

public abstract class GhCliAuthBase {
    internal companion object {
        const val GH_CLI_EXTENSION_NAME: String = "ghCliAuth"
        const val GH_ORG_SETTER_PROPERTY: String = "gh.cli.auth.github.org"
        const val GH_ENV_KEY_SETTER_PROPERTY: String = "gh.cli.auth.env.name"
        const val GH_PROPERTY_KEY_SETTER_PROPERTY: String = "gh.cli.auth.property.name"
        const val GH_EXTRA_TOKEN_KEY: String = "gh.cli.auth.token"

        const val DEFAULT_TOKEN_ENV_KEY: String = "GITHUB_TOKEN"
        const val DEFAULT_TOKEN_PROPERTY_KEY: String = "gpr.token"
        const val DEFAULT_TOKEN_USERNAME: String = ""
    }

    private val githubOrgCache = ConcurrentHashMap<ProviderFactory, String>()
    private val credentialsCache = ConcurrentHashMap<ProviderFactory, GhCredentials>()

    protected val logger: Logger = Logging.getLogger(GH_CLI_EXTENSION_NAME)

    protected val ProviderFactory.githubOrg: String
        get() = githubOrgCache.getOrPut(this) {
            this
                .gradleProperty(GH_ORG_SETTER_PROPERTY)
                .orNull
                .let { requireNotNull(it) { "Please set '${GH_ORG_SETTER_PROPERTY}' in gradle.properties." } }
                .also { require(it.isNotBlank()) { "Property '${GH_ORG_SETTER_PROPERTY}' MUST not be blank." } }
        }

    protected val ProviderFactory.credentials: GhCredentials
        get() = credentialsCache.getOrPut(this) {
            val tokenEnvKey = this
                .gradleProperty(GH_ENV_KEY_SETTER_PROPERTY)
                .orNull ?: DEFAULT_TOKEN_ENV_KEY

            val credentialsFromEnv = System
                .getenv(tokenEnvKey)
                .let { token -> if (token.isNullOrEmpty().not()) GhCredentials(DEFAULT_TOKEN_USERNAME, token) else null }

            if (credentialsFromEnv != null) {
                logger.debug("Attempting to use GitHub credentials from environment variable: $tokenEnvKey")
                return@getOrPut credentialsFromEnv
            } else {
                val tokenPropertyKey = this
                    .gradleProperty(GH_PROPERTY_KEY_SETTER_PROPERTY)
                    .orNull ?: DEFAULT_TOKEN_PROPERTY_KEY

                val credentialsFromProperty = this
                    .gradleProperty(tokenPropertyKey)
                    .orNull
                    .let { token -> if (!token.isNullOrBlank()) GhCredentials(DEFAULT_TOKEN_USERNAME, token) else null }

                if (credentialsFromProperty != null) {
                    logger.debug("Attempting to use GitHub credentials from gradle property: $tokenPropertyKey")
                    return@getOrPut credentialsFromProperty
                } else {
                    logger.debug("Attempting to use GitHub credentials from gh CLI.")
                    return@getOrPut GhCliAuthProcessor
                        .create(this)
                        // We collect (i.e., `.get()`) the value before validation to ensure
                        // the final side effects of the provider are executed
                        .get()
                        .run(GhCliAuthParser::parse)
                }
            }
        }

    protected fun RepositoryHandler.addTrustedRepositoriesIfMissing() {
        if (this.findByName("Gradle Central Plugin Repository") == null) {
            logger.info("Adding Gradle Plugin Portal repository")
            this.gradlePluginPortal()
        }
    }

    protected fun RepositoryHandler.addUserConfiguredOrgGhPackagesRepository(providers: ProviderFactory) {
        logger.info("Registering GitHub Packages maven repository for organization: ${providers.githubOrg}")
        maven {
            name = providers.githubOrg
            url = URI("https://maven.pkg.github.com/${providers.githubOrg}/*")
            credentials {
                username = providers.credentials.username
                password = providers.credentials.token
            }
        }
    }
}
