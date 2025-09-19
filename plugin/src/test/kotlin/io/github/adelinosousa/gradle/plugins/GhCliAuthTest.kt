package io.github.adelinosousa.gradle.plugins

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import org.gradle.api.GradleException
import org.gradle.api.Transformer
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class GhCliAuthTest {
    val providerFactory = mockk<ProviderFactory>(relaxed = true)
    val cliProcessProvider = mockk<Provider<String>>(relaxed = true)

    @AfterEach fun tearDown() {
        unmockkAll()
    }

    @Test fun `checkGhCliAuthenticatedWithCorrectScopes throws exception when gh CLI is not installed`() {
        val project = ProjectBuilder.builder().build()
        val spyProject = spyk(project)

        every { spyProject.providers } returns providerFactory

        val exception = GradleException("Failed to authenticate: GitHub CLI is not installed or not found in PATH. Please install it before using this plugin.")
        every { cliProcessProvider.get() } throws exception
        every { cliProcessProvider.map(any<Transformer<String, String>>())} returns cliProcessProvider
        every { providerFactory.of(eq(GitHubCLIProcess::class.java), any()) } returns cliProcessProvider

        val result = assertThrows<GradleException> {
            val authStatusProvider = spyProject.providers.of(GitHubCLIProcess::class.java) {}
            GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes(authStatusProvider).get()
        }

        assertEquals("Failed to authenticate: GitHub CLI is not installed or not found in PATH. Please install it before using this plugin.", result.message)
    }

    @Test fun `checkGhCliAuthenticatedWithCorrectScopes throws exception when gh CLI is not authenticated`() {
        val project = ProjectBuilder.builder().build()
        val spyProject = spyk(project)

        every { spyProject.providers } returns providerFactory

        val exception = GradleException("Failed to authenticate: GitHub CLI is not authenticated or does not have the required scopes ${GhCliAuth.requiredScopes}")
        every { cliProcessProvider.get() } throws exception
        every { cliProcessProvider.map(any<Transformer<String, String>>())} returns cliProcessProvider
        every { providerFactory.of(eq(GitHubCLIProcess::class.java), any()) } returns cliProcessProvider

        val result = assertThrows<GradleException> {
            val authStatusProvider = spyProject.providers.of(GitHubCLIProcess::class.java) {}
            GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes(authStatusProvider).get()
        }
        assertEquals("Failed to authenticate: GitHub CLI is not authenticated or does not have the required scopes ${GhCliAuth.requiredScopes}", result.message)
    }

    @Test fun `checkGhCliAuthenticatedWithCorrectScopes throws exception when gh CLI does not have required scopes`() {
        val project = ProjectBuilder.builder().build()
        val spyProject = spyk(project)

        every { spyProject.providers } returns providerFactory

        val exception = GradleException("Failed to authenticate: GitHub CLI is not authenticated or does not have the required scopes ${GhCliAuth.requiredScopes}")

        val output = """
            Logged in to github.com account testuser (keyring)
            Token: ghs_1234567890abcdef
            Token scopes: 'repo', 'user'
        """.trimIndent()
        every { cliProcessProvider.get() } returns output
        every { cliProcessProvider.map(any<Transformer<String, String>>())} throws exception
        every { providerFactory.of(eq(GitHubCLIProcess::class.java), any()) } returns cliProcessProvider

        val result = assertThrows<GradleException> {
            val authStatusProvider = spyProject.providers.of(GitHubCLIProcess::class.java) {}
            GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes(authStatusProvider).get()
        }

        assertEquals("Failed to authenticate: GitHub CLI is not authenticated or does not have the required scopes ${GhCliAuth.requiredScopes}", result.message)
    }

    @Test fun `checkGhCliAuthenticatedWithCorrectScopes returns output when authenticated with required scopes`() {
        val project = ProjectBuilder.builder().build()
        val spyProject = spyk(project)

        every { spyProject.providers } returns providerFactory

        val expectedOutput = """
            Logged in to github.com account testuser (keyring)
            Token: ghs_1234567890abcdef
            Token scopes: 'read:packages', 'repo', 'read:org', 'user'
        """.trimIndent()

        every { cliProcessProvider.get() } returns expectedOutput
        every { cliProcessProvider.map(any<Transformer<String, String>>())} returns cliProcessProvider
        every { providerFactory.of(eq(GitHubCLIProcess::class.java), any()) } returns cliProcessProvider

        val authStatusProvider = spyProject.providers.of(GitHubCLIProcess::class.java) {}
        val result = GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes(authStatusProvider).get()

        assertEquals(expectedOutput, result)
    }
}