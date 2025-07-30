package io.github.adelinosousa.gradle.plugins

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
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

class GhCliAuthPluginTest {
    val testOrg = "test-org"
    val testUsername = "test-user"
    val testToken = "test-token"

    @BeforeTest
    fun setUp() {
        mockkObject(GhCliAuth)
        every { GhCliAuth.log(any()) } returns Unit
        every { GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes() } returns "Authenticated"
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test fun `plugin applies and creates extension`() {
        val project = ProjectBuilder.builder().build()
        val spyProject = spyk(project)

        every { GhCliAuth.getGitHubCredentials(any(), any()) } returns (testUsername to testToken)
        every { spyProject.providers.gradleProperty(GhCliAuth.GITHUB_ORG) } returns Providers.of(testOrg)
        every { spyProject.providers.gradleProperty(GhCliAuth.ENV_PROPERTY_NAME) } returns Providers.of("")

        GhCliAuthProjectPlugin().apply(spyProject)

        assertNotNull(project.extensions.findByName("ghCliAuth"))
    }

    @Test fun `configures maven repository with gh CLI credentials`() {
        val project = ProjectBuilder.builder().build()
        val spyProject = spyk(project)

        every { GhCliAuth.getGitHubCredentials(any(), any()) } returns (testUsername to testToken)
        every { spyProject.providers.gradleProperty(GhCliAuth.GITHUB_ORG) } returns Providers.of(testOrg)
        every { spyProject.providers.gradleProperty(GhCliAuth.ENV_PROPERTY_NAME) } returns Providers.of("")

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

        every { GhCliAuth.getGitHubCredentials(any(), any()) } returns (null to null)
        every { spyProject.providers.gradleProperty(GhCliAuth.GITHUB_ORG) } returns Providers.of(testOrg)
        every { spyProject.providers.gradleProperty(GhCliAuth.ENV_PROPERTY_NAME) } returns Providers.of("")

        val exception = assertThrows<IllegalStateException> {
            GhCliAuthProjectPlugin().apply(spyProject)
        }

        val repo = project.repositories.findByName("GitHubPackages")
        assertNull(repo)
        assertEquals("GitHub token not found. Unable to configure GitHub Packages repository.", exception.message)
    }

    @Test fun `does not configure repository when github org property is missing`() {
        val project = ProjectBuilder.builder().build()
        val spyProject = spyk(project)

        every { GhCliAuth.getGitHubCredentials(any(), any()) } returns (testUsername to testToken)
        every { spyProject.providers.gradleProperty(GhCliAuth.GITHUB_ORG) } returns Providers.of("")
        every { spyProject.providers.gradleProperty(GhCliAuth.ENV_PROPERTY_NAME) } returns Providers.of("")

        val exception = assertThrows<IllegalStateException> {
            GhCliAuthProjectPlugin().apply(spyProject)
        }

        val repo = project.repositories.findByName("GitHubPackages")
        assertNull(repo)
        assertEquals("GitHub token not found. Unable to configure GitHub Packages repository.", exception.message)
    }
}
