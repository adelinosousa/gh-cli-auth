package org.gradle.kotlin.dsl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency
import org.junit.jupiter.api.Test

class ProjectExtTest {

    @Test
    fun `enable applies plugin using pluginId from PluginDependency alias`() {
        val project = mockk<Project>(relaxed = true)
        val rootProject = mockk<Project>(relaxed = true)
        val provider = mockk<Provider<PluginDependency>>()
        val pluginDependency = mockk<PluginDependency>()
        val actionSlot: CapturingSlot<Action<in Any>> = slot()

        every { project.rootProject } returns rootProject
        every { provider.get() } returns pluginDependency
        every { pluginDependency.pluginId } returns "com.example.plugin"
        every { project.apply(capture(actionSlot)) } returns Unit

        project.enable { provider }

        verify { project.apply(any<Action<in Any>>()) }
    }

    @Test
    fun `enable throws error when alias is not PluginDependency`() {
        val project = mockk<Project>(relaxed = true)
        val rootProject = mockk<Project>(relaxed = true)
        val provider = mockk<Provider<String>>()

        every { project.rootProject } returns rootProject
        every { provider.get() } returns "not-a-plugin-dependency"

        shouldThrow<IllegalStateException> {
            project.enable { provider }
        }.message shouldBe "Cannot extract pluginId from alias: not-a-plugin-dependency"
    }
}
