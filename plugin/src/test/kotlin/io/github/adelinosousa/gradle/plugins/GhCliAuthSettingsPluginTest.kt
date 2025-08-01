package io.github.adelinosousa.gradle.plugins

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.initialization.Settings
import org.gradle.api.internal.provider.Providers
import org.junit.jupiter.api.assertThrows
import java.net.URI
import kotlin.IllegalStateException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GhCliAuthSettingsPluginTest {
    val testOrg = "test-org"
    val testUsername = "test-user"
    val testToken = "test-token"
    val settings = mockk<Settings>(relaxed = true)
    val mavenAction = slot<Action<in MavenArtifactRepository>>()
    val mockRepo = mockk<MavenArtifactRepository>(relaxed = true)
    val mockCredentials = mockk<PasswordCredentials>(relaxed = true)

    @BeforeTest fun setUp() {
        mockkObject(GhCliAuth, Environment)
    }

    @AfterTest fun tearDown() {
        unmockkAll()
    }

    @Test fun `configures maven repository with environment variables`() {
        val customEnvName = "CUSTOM_GITHUB_TOKEN"

        every { settings.providers.gradleProperty(Config.GITHUB_ORG) } returns Providers.of(testOrg)
        every { settings.providers.gradleProperty(Config.ENV_PROPERTY_NAME) } returns Providers.of(customEnvName)
        every { Environment.getEnvCredentials(customEnvName) } returns RepositoryCredentials(username = "", token = testToken)
        every { settings.pluginManagement.repositories.maven(capture(mavenAction)) } returns mockk()
        every { mockRepo.credentials(any<Action<in PasswordCredentials>>()) } answers {
            val credentialsAction = firstArg<Action<in PasswordCredentials>>()
            credentialsAction.execute(mockCredentials)
        }

        GhCliAuthSettingsPlugin().apply(settings)

        mavenAction.captured.execute(mockRepo)

        verify {
            mockRepo.name = "GitHubPackages"
            mockRepo.url = URI("https://maven.pkg.github.com/$testOrg/*")
            mockCredentials.username = ""
            mockCredentials.password = testToken
        }
    }

    @Test fun `configures maven repository with gh CLI credentials`() {
        every { GhCliAuth.getGitHubCredentials(any()) } returns RepositoryCredentials(testUsername, testToken)
        every { settings.providers.gradleProperty(Config.GITHUB_ORG) } returns Providers.of(testOrg)
        every { settings.providers.gradleProperty(Config.ENV_PROPERTY_NAME) } returns Providers.of("")
        every { settings.pluginManagement.repositories.maven(capture(mavenAction)) } returns mockk()
        every { mockRepo.credentials(any<Action<in PasswordCredentials>>()) } answers {
            val credentialsAction = firstArg<Action<in PasswordCredentials>>()
            credentialsAction.execute(mockCredentials)
        }

        GhCliAuthSettingsPlugin().apply(settings)

        mavenAction.captured.execute(mockRepo)

        verify {
            mockRepo.name = "GitHubPackages"
            mockRepo.url = URI("https://maven.pkg.github.com/$testOrg/*")
            mockCredentials.username = testUsername
            mockCredentials.password = testToken
        }
    }

    @Test fun `throws error when repository is not configured with gh CLI credentials`() {
        every { GhCliAuth.getGitHubCredentials(any()) } returns RepositoryCredentials(null, null)
        every { settings.providers.gradleProperty(Config.GITHUB_ORG) } returns Providers.of(testOrg)
        every { settings.providers.gradleProperty(Config.ENV_PROPERTY_NAME) } returns Providers.of("")

        val exception = assertThrows<IllegalStateException> {
            GhCliAuthSettingsPlugin().apply(settings)
        }

        assertEquals("Token not found in environment variable '' or 'gh' CLI. Unable to configure GitHub Packages repository.", exception.message)
    }

    @Test fun `does not configure repository when github org property is missing`() {
        every { GhCliAuth.getGitHubCredentials(any()) } returns RepositoryCredentials(testUsername, testToken)
        every { settings.providers.gradleProperty(Config.GITHUB_ORG) } returns Providers.of("")
        every { settings.providers.gradleProperty(Config.ENV_PROPERTY_NAME) } returns Providers.of("")

        val exception = assertThrows<IllegalStateException> {
            GhCliAuthSettingsPlugin().apply(settings)
        }

        assertEquals("GitHub organization not specified. Please set the '${Config.GITHUB_ORG}' in your gradle.properties file.", exception.message)
    }
}
