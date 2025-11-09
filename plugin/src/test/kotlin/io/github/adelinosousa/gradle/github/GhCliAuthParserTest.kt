package io.github.adelinosousa.gradle.github

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class GhCliAuthParserTest {

    private fun sampleOutput(
        user: String? = "octocat",
        token: String? = "ghp_exampletoken123",
        scopes: String = "'read:packages', 'read:org'"
    ): String = buildString {
        appendLine("gh auth status")
        appendLine("Logged in to github.com")
        if (user != null) appendLine("Logged in to github.com account $user (keyring)")
        appendLine("Token scopes: $scopes")
        if (token != null) appendLine("Token: $token")
        appendLine("Config dir: /Users/octo/.config/gh")
    }

    @Test
    fun `parse extracts username and token when output is valid`() {
        val output = sampleOutput(
            user = "octocat",
            token = "ghp_abcdef1234567890",
            scopes = "'workflow', 'read:packages', 'read:org'"
        )

        val creds = GhCliAuthParser.parse(output)

        creds.username shouldBe "octocat"
        creds.token shouldBe "ghp_abcdef1234567890"
    }

    @Test
    fun `parse trims keyring suffix and whitespace`() {
        val output = buildString {
            appendLine("Some header")
            appendLine("Logged in to github.com account    octo-user    (keyring)   ")
            appendLine("Token scopes:   'read:packages' ,  'read:org'  ")
            appendLine("Token:   ghp_trim_me   ")
        }

        val creds = GhCliAuthParser.parse(output)

        creds.username shouldBe "octo-user"
        creds.token shouldBe "ghp_trim_me"
    }

    @Test
    fun `parse fails when required scopes are missing`() {
        val output = sampleOutput(
            user = "octocat",
            token = "ghp_no_scopes",
            scopes = "'read:packages'" // missing 'read:org'
        )

        val ex = shouldThrow<IllegalStateException> {
            GhCliAuthParser.parse(output)
        }

        ex.message.shouldContain("GitHub CLI token is missing required scopes")
        ex.message.shouldContain("read:packages")
        ex.message.shouldContain("read:org")
    }

    @Test
    fun `parse fails when Token scopes line is absent`() {
        val output = buildString {
            appendLine("gh auth status")
            appendLine("Logged in to github.com account octocat (keyring)")
            // No "Token scopes:" line at all
            appendLine("Token: ghp_token_but_no_scopes")
        }

        val ex = shouldThrow<IllegalStateException> {
            GhCliAuthParser.parse(output)
        }

        ex.message.shouldContain("GitHub CLI token is missing required scopes")
    }

    @Test
    fun `parse fails with generic message when username is missing`() {
        val output = sampleOutput(
            user = null, // remove username line
            token = "ghp_missing_user",
            scopes = "'read:packages', 'read:org'"
        )

        val ex = shouldThrow<IllegalStateException> {
            GhCliAuthParser.parse(output)
        }

        ex.message shouldBe "'gh' CLI is authenticated but failed to extract user or token"
    }

    @Test
    fun `parse fails with generic message when token is missing`() {
        val output = sampleOutput(
            user = "octocat",
            token = null, // remove token line
            scopes = "'read:packages', 'read:org'"
        )

        val ex = shouldThrow<IllegalStateException> {
            GhCliAuthParser.parse(output)
        }

        ex.message shouldBe "'gh' CLI is authenticated but failed to extract user or token"
    }

    @Test
    fun `parse accepts scopes in any order and with quotes`() {
        val output = sampleOutput(
            user = "someone",
            token = "ghp_ok",
            scopes = "'read:org', 'workflow', 'read:packages'"
        )

        val creds = GhCliAuthParser.parse(output)

        creds.username shouldBe "someone"
        creds.token shouldBe "ghp_ok"
    }
}
