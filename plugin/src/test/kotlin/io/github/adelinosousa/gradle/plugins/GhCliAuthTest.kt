package io.github.adelinosousa.gradle.plugins

import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.gradle.api.GradleException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class GhCliAuthTest {
    private val mockProcess = mockk<GitHubCLIProcessProvider>()
    private val originalProcessProvider = GhCliAuth.ghCLIProcess

    @BeforeEach fun setUp() {
        GhCliAuth.ghCLIProcess = mockProcess
    }

    @AfterEach fun tearDown() {
        GhCliAuth.ghCLIProcess = originalProcessProvider
        unmockkAll()
    }

    @Test fun `checkGhCliAuthenticatedWithCorrectScopes throws exception when gh CLI is not installed`() {
        every { mockProcess.isGhCliInstalled() } throws IOException()

        val result = assertThrows<GradleException> {
            GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes()
        }

        assertEquals("Failed to authenticate: GitHub CLI is not installed or not found in PATH. Please install it before using this plugin.", result.message)
    }

    @Test fun `checkGhCliAuthenticatedWithCorrectScopes throws exception when gh CLI is not authenticated`() {
        every { mockProcess.isGhCliInstalled() } returns true
        every { mockProcess.getGhCliAuthStatus() } returns "Not logged in to GitHub CLI"
        val result = assertThrows<GradleException> {
            GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes()
        }
        assertEquals("Failed to authenticate: GitHub CLI is not authenticated or does not have the required scopes ${GhCliAuth.requiredScopes}", result.message)
    }

    @Test fun `checkGhCliAuthenticatedWithCorrectScopes throws exception when gh CLI does not have required scopes`() {
        every { mockProcess.isGhCliInstalled() } returns true
        every { mockProcess.getGhCliAuthStatus() } returns """
            Logged in to github.com account testuser (keyring)
            Token: ghs_1234567890abcdef
            Token scopes: 'repo', 'user'
        """.trimIndent()

        val result = assertThrows<GradleException> {
            GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes()
        }

        assertEquals("Failed to authenticate: GitHub CLI is not authenticated or does not have the required scopes ${GhCliAuth.requiredScopes}", result.message)
    }

    @Test fun `checkGhCliAuthenticatedWithCorrectScopes returns output when authenticated with required scopes`() {
        val expectedOutput = """
            Logged in to github.com account testuser (keyring)
            Token: ghs_1234567890abcdef
            Token scopes: 'read:packages', 'repo', 'read:org', 'user'
        """.trimIndent()

        every { mockProcess.isGhCliInstalled() } returns true
        every { mockProcess.getGhCliAuthStatus() } returns expectedOutput

        val result = GhCliAuth.checkGhCliAuthenticatedWithCorrectScopes()

        assertEquals(expectedOutput, result)
    }

    @Test fun `getGitHubCredentials throws exception when gh CLI auth output is not in expected format`() {
        val invalidOutput = "'gh' CLI output changed"

        val result = assertThrows<GradleException> {
            GhCliAuth.getGitHubCredentials(invalidOutput)
        }

        assertEquals("'gh' CLI is authenticated but failed to extract user or token", result.message)
    }
}