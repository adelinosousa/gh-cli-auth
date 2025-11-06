package io.github.adelinosousa.gradle.plugins.support

import java.io.File

/**
 * Fake GitHub CLI authentication script for functional tests.
 *
 * This class creates a fake `gh` CLI script that simulates authentication
 * by outputting a predefined token and scopes.
 */
class GhCliAuthFake(
    private val projectDir: File
) {
    companion object {
        internal const val DEFAULT_USER_VALUE: String = "testuser"
        internal const val DEFAULT_TOKEN_VALUE: String = "ghp_mocked_token_1234567890"
        internal val DEFAULT_VALID_SCOPES: List<String> = listOf("read:packages", "read:org", "repo")
    }

    internal lateinit var fakeGhScript: File
        private set

    internal fun execute(
        token: String = DEFAULT_TOKEN_VALUE,
        validScopes: List<String> = DEFAULT_VALID_SCOPES,
        username: String = DEFAULT_USER_VALUE
    ) {
        fakeGhScript = projectDir
            .resolve("bin")
            .apply { mkdirs() }
            .resolve("gh")
            .apply {
                writeText(
                    """
                    #!/bin/bash
                    echo "Logged in to github.com account $username (keyring)"
                    echo "Token: $token"
                    echo "Token scopes: ${validScopes.joinToString(", ")}"
                    exit 0
                    """
                )
            }

        ProcessBuilder("chmod", "+x", fakeGhScript.absolutePath)
            .start()
            .waitFor()
    }
}