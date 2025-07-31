package io.github.adelinosousa.gradle.plugins

import java.io.ByteArrayOutputStream

class GitHubCLIProcess : GitHubCLIProcessProvider {
    override fun isGhCliInstalled() = ProcessBuilder("gh", "--version").start().waitFor() == 0

    override fun getGhCliAuthStatus(): String {
        val process = ProcessBuilder("gh", "auth", "status", "--show-token").start()
        val outputStream = ByteArrayOutputStream()
        process.inputStream.copyTo(outputStream)
        val output = outputStream.toString().trim()

        if (process.waitFor() == 0) {
            return output
        }

        throw IllegalStateException("Failed to execute 'gh auth status'")
    }
}

interface GitHubCLIProcessProvider {
    fun isGhCliInstalled(): Boolean
    fun getGhCliAuthStatus(): String
}

