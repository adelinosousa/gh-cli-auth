package io.github.adelinosousa.gradle.plugins

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import org.gradle.api.GradleException
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.provider.Providers
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.assertThrows
import kotlin.IllegalStateException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GhCliAuthProjectPluginTest {
    val testOrg = "test-org"
    val testUsername = "test-user"
    val testToken = "test-token"

    @BeforeTest fun setUp() {
        mockkObject(GhCliAuth, Environment)
    }

    @AfterTest fun tearDown() {
        unmockkAll()
    }

    @Test fun `plugin applies and creates extension`() {
        val project = ProjectBuilder.builder().build()
        val spyProject = spyk(project)

        every { GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes() } returns ""
        every { GhCliAuth.getGitHubCredentials(any()) } returns RepositoryCredentials(testUsername, testToken)
        every { spyProject.providers.gradleProperty(Config.GITHUB_ORG) } returns Providers.of(testOrg)
        every { spyProject.providers.gradleProperty(Config.ENV_PROPERTY_NAME) } returns Providers.of("")

        GhCliAuthProjectPlugin().apply(spyProject)

        assertNotNull(project.extensions.findByName("ghCliAuth"))
    }

    @Test fun `configures maven repository with environment variables`() {
        val project = ProjectBuilder.builder().build()
        val spyProject = spyk(project)
        val customEnvName = "CUSTOM_GITHUB_TOKEN"

        every { spyProject.providers.gradleProperty(Config.GITHUB_ORG) } returns Providers.of(testOrg)
        every { spyProject.providers.gradleProperty(Config.ENV_PROPERTY_NAME) } returns Providers.of(customEnvName)
        every { Environment.getEnv(customEnvName) } returns testToken

        GhCliAuthProjectPlugin().apply(spyProject)

        val repo = spyProject.repositories.findByName("GitHubPackages") as? MavenArtifactRepository
        assertNotNull(repo)
        assertEquals("https://maven.pkg.github.com/${testOrg}/*", repo.url.toString())
        assertEquals("", repo.credentials.username)
        assertEquals(testToken, repo.credentials.password)

        val extension = spyProject.extensions.getByType(GhCliAuthExtension::class.java)
        assertEquals(testToken, extension.token.get())
    }

    @Test fun `configures maven repository with gh CLI credentials`() {
        val project = ProjectBuilder.builder().build()
        val spyProject = spyk(project)

        every { GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes() } returns ""
        every { GhCliAuth.getGitHubCredentials(any()) } returns RepositoryCredentials(testUsername, testToken)
        every { spyProject.providers.gradleProperty(Config.GITHUB_ORG) } returns Providers.of(testOrg)
        every { spyProject.providers.gradleProperty(Config.ENV_PROPERTY_NAME) } returns Providers.of("")

        GhCliAuthProjectPlugin().apply(spyProject)

        val repo = spyProject.repositories.findByName("GitHubPackages") as? MavenArtifactRepository
        assertNotNull(repo)
        assertEquals("https://maven.pkg.github.com/${testOrg}/*", repo.url.toString())
        assertEquals(testUsername, repo.credentials.username)
        assertEquals(testToken, repo.credentials.password)

        val extension = spyProject.extensions.getByType(GhCliAuthExtension::class.java)
        assertEquals(testToken, extension.token.get())
    }

    @Test fun `throws error when repository is not configured with gh CLI credentials`() {
        val project = ProjectBuilder.builder().build()
        val spyProject = spyk(project)

        every { GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes() } returns ""
        every { GhCliAuth.getGitHubCredentials(any()) } returns RepositoryCredentials(null, null)
        every { spyProject.providers.gradleProperty(Config.GITHUB_ORG) } returns Providers.of(testOrg)
        every { spyProject.providers.gradleProperty(Config.ENV_PROPERTY_NAME) } returns Providers.of("")

        val exception = assertThrows<IllegalStateException> {
            GhCliAuthProjectPlugin().apply(spyProject)
        }

        val repo = project.repositories.findByName("GitHubPackages")
        assertNull(repo)
        assertEquals("Token not found in environment variable '' or 'gh' CLI. Unable to configure GitHub Packages repository.", exception.message)
    }

    @Test fun `throws error when gh CLI is not installed`() {
        val exceptionMessage = "Failed to authenticate: GitHub CLI is not installed or not found in PATH. Please install it before using this plugin."
        val project = ProjectBuilder.builder().build()
        val spyProject = spyk(project)

        every { GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes() } throws GradleException(exceptionMessage)
        every { spyProject.providers.gradleProperty(Config.GITHUB_ORG) } returns Providers.of(testOrg)
        every { spyProject.providers.gradleProperty(Config.ENV_PROPERTY_NAME) } returns Providers.of("")

        val exception = assertThrows<GradleException> {
            GhCliAuthProjectPlugin().apply(spyProject)
        }

        assertEquals(exceptionMessage, exception.message)
    }

    @Test fun `does not configure repository when github org property is missing`() {
        val project = ProjectBuilder.builder().build()
        val spyProject = spyk(project)

        every { GhCliAuth.getGitHubCredentials(any()) } returns RepositoryCredentials(testUsername, testToken)
        every { spyProject.providers.gradleProperty(Config.GITHUB_ORG) } returns Providers.of("")
        every { spyProject.providers.gradleProperty(Config.ENV_PROPERTY_NAME) } returns Providers.of("")

        val exception = assertThrows<IllegalStateException> {
            GhCliAuthProjectPlugin().apply(spyProject)
        }

        val repo = project.repositories.findByName("GitHubPackages")
        assertNull(repo)
        assertEquals("GitHub organization not specified. Please set the '${Config.GITHUB_ORG}' in your gradle.properties file.", exception.message)
    }
}
