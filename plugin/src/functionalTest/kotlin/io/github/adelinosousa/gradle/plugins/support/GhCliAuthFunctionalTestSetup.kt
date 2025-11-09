package io.github.adelinosousa.gradle.plugins.support

import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir

abstract class GhCliAuthFunctionalTestSetup {
    companion object {
        const val GIVEN_ORG_VALUE = "test-org"
    }

    @field:TempDir
    lateinit var projectDir: File

    lateinit var project: GradleRunner

    val buildFile by lazy {
        projectDir.resolve("build.gradle.kts")
    }

    val settingsFile by lazy {
        projectDir.resolve("settings.gradle.kts")
    }

    val propertiesFile by lazy {
        projectDir.resolve("gradle.properties")
    }

    val fakeGhExtension by lazy {
        GhCliAuthFake(projectDir).apply { execute() }
    }

    @BeforeEach
    fun `configure project defaults`() {
        propertiesFile.writeText(
            """
            gh.cli.auth.github.org=$GIVEN_ORG_VALUE
            systemProp.gh.cli.binary.path=${fakeGhExtension.fakeGhScript.absolutePath}
            """.trimIndent()
        )

        settingsFile.createNewFile()
        buildFile.createNewFile()

        project = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("--info")
            .withGradleVersion("8.14.2")
    }

}