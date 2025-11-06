package org.gradle.kotlin.dsl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class SettingsExtTest {
    @Test
    fun `includeModule includes project and sets name and directory`() {
        val settings = mockk<Settings>(relaxed = true)
        val projectDescriptor = mockk<ProjectDescriptor>(relaxed = true)
        val rootDir = mockk<File>()
        val rootPath = mockk<Path>()
        val modulesPath = mockk<Path>()
        val moduleDirPath = mockk<Path>()
        val moduleFile = mockk<File>()

        every { settings.rootDir } returns rootDir
        every { rootDir.toPath() } returns rootPath
        every { rootPath.resolve("modules") } returns modulesPath
        every { modulesPath.resolve("my-module") } returns moduleDirPath
        every { moduleDirPath.toFile() } returns moduleFile
        every { settings.project(":my-module") } returns projectDescriptor

        settings.includeModule("my-module", "prefix-")

        verify { settings.include(":my-module") }
        verify { projectDescriptor.name = "prefix-my-module" }
    }

    @Test
    fun `includeModule without prefix uses module name as is`() {
        val settings = mockk<Settings>(relaxed = true)
        val projectDescriptor = mockk<ProjectDescriptor>(relaxed = true)
        val rootDir = mockk<File>()
        val rootPath = mockk<Path>()
        val modulesPath = mockk<Path>()
        val moduleDirPath = mockk<Path>()
        val moduleFile = mockk<File>()

        every { settings.rootDir } returns rootDir
        every { rootDir.toPath() } returns rootPath
        every { rootPath.resolve("modules") } returns modulesPath
        every { modulesPath.resolve("core") } returns moduleDirPath
        every { moduleDirPath.toFile() } returns moduleFile
        every { settings.project(":core") } returns projectDescriptor

        settings.includeModule("core")

        verify { projectDescriptor.name = "core" }
    }
}
