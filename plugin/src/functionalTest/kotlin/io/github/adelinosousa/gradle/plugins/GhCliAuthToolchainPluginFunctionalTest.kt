package io.github.adelinosousa.gradle.plugins

import io.github.adelinosousa.gradle.plugins.support.GhCliAuthFunctionalTestSetup
import io.github.adelinosousa.gradle.tasks.GhCliAuthInstallTask
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.Test
import java.io.File

class GhCliAuthToolchainPluginFunctionalTest : GhCliAuthFunctionalTestSetup() {

    private fun fakeGradleUserHome(): File =
        projectDir.resolve(".gradle-home").apply { mkdirs() }

    private fun initScriptFile(gradleHome: File): File =
        gradleHome.resolve("init.d/${GhCliAuthInstallTask.INIT_SCRIPT_NAME}")

    private fun writeBuildWithToolchainPlugin(gradleHome: File) {
        buildFile.writeText(
            """
            plugins {
                id("io.github.adelinosousa.gradle.plugins.toolchain.gh-cli-auth")
            }

            tasks.named<io.github.adelinosousa.gradle.tasks.GhCliAuthInstallTask>("ghCliAuthInstall") {
                gradleUserHomeDir.set("${gradleHome.absolutePath}")
            }

            tasks.named<io.github.adelinosousa.gradle.tasks.GhCliAuthUninstallTask>("ghCliAuthUninstall") {
                gradleUserHomeDir.set("${gradleHome.absolutePath}")
            }
            """.trimIndent()
        )
    }

    @Test
    fun `should apply toolchain plugin without settings or project plugin`() {
        val gradleHome = fakeGradleUserHome()
        writeBuildWithToolchainPlugin(gradleHome)

        project
            .withArguments("tasks", "--group=gh-cli-auth")
            .build()
            .output
            .shouldContain("ghCliAuthInstall")
            .shouldContain("ghCliAuthUninstall")
    }

    @Test
    fun `should create init script in gradle user home`() {
        val gradleHome = fakeGradleUserHome()
        writeBuildWithToolchainPlugin(gradleHome)

        project
            .withArguments("ghCliAuthInstall")
            .build()

        initScriptFile(gradleHome).exists().shouldBeTrue()
    }

    @Test
    fun `should bake org into init script from gradle property`() {
        val gradleHome = fakeGradleUserHome()
        writeBuildWithToolchainPlugin(gradleHome)

        project
            .withArguments("ghCliAuthInstall")
            .build()

        val content = initScriptFile(gradleHome).readText()
        content.shouldContain("https://maven.pkg.github.com/$GIVEN_ORG_VALUE/*")
    }

    @Test
    fun `should include inline gh auth token logic in init script`() {
        val gradleHome = fakeGradleUserHome()
        writeBuildWithToolchainPlugin(gradleHome)

        project
            .withArguments("ghCliAuthInstall")
            .build()

        val content = initScriptFile(gradleHome).readText()
        content.shouldContain("gh")
        content.shouldContain("auth")
        content.shouldContain("token")
    }

    @Test
    fun `should not contain hardcoded tokens in init script`() {
        val gradleHome = fakeGradleUserHome()
        writeBuildWithToolchainPlugin(gradleHome)

        project
            .withArguments("ghCliAuthInstall")
            .build()

        val content = initScriptFile(gradleHome).readText()
        content.shouldNotContain("ghp_")
    }

    @Test
    fun `should be idempotent when run multiple times`() {
        val gradleHome = fakeGradleUserHome()
        writeBuildWithToolchainPlugin(gradleHome)

        project.withArguments("ghCliAuthInstall").build()
        val firstContent = initScriptFile(gradleHome).readText()

        project.withArguments("ghCliAuthInstall").build()
        val secondContent = initScriptFile(gradleHome).readText()

        firstContent.shouldContain(secondContent)
        secondContent.shouldContain(firstContent)
    }

    @Test
    fun `should fail when org is not set in gradle properties`() {
        val gradleHome = fakeGradleUserHome()

        buildFile.writeText(
            """
            plugins {
                id("io.github.adelinosousa.gradle.plugins.toolchain.gh-cli-auth")
            }

            tasks.named<io.github.adelinosousa.gradle.tasks.GhCliAuthInstallTask>("ghCliAuthInstall") {
                gradleUserHomeDir.set("${gradleHome.absolutePath}")
            }
            """.trimIndent()
        )

        propertiesFile.writeText(
            propertiesFile.readText()
                .lines()
                .filterNot { it.startsWith("gh.cli.auth.github.org") }
                .joinToString("\n")
        )

        project
            .withArguments("ghCliAuthInstall")
            .buildAndFail()
    }

    @Test
    fun `should configure pluginManagement repositories in the generated init script`() {
        val gradleHome = fakeGradleUserHome()
        writeBuildWithToolchainPlugin(gradleHome)

        project
            .withArguments("ghCliAuthInstall")
            .build()

        val content = initScriptFile(gradleHome).readText()
        content.shouldContain("pluginManagement")
        content.shouldContain("repositories")
        content.shouldContain("maven")
    }

    @Test
    fun `should remove init script when it exists`() {
        val gradleHome = fakeGradleUserHome()
        writeBuildWithToolchainPlugin(gradleHome)

        project.withArguments("ghCliAuthInstall").build()
        initScriptFile(gradleHome).exists().shouldBeTrue()

        project.withArguments("ghCliAuthUninstall").build()
        initScriptFile(gradleHome).exists().shouldBeFalse()
    }

    @Test
    fun `should succeed uninstall even when init script does not exist`() {
        val gradleHome = fakeGradleUserHome()
        writeBuildWithToolchainPlugin(gradleHome)

        initScriptFile(gradleHome).exists().shouldBeFalse()

        project
            .withArguments("ghCliAuthUninstall")
            .build()
    }
}
