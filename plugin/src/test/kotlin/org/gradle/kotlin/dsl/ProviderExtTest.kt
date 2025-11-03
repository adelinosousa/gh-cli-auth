package org.gradle.kotlin.dsl

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.assertEquals
import org.gradle.api.Transformer
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ProviderExtTest {
    @Test
    fun `asDependency maps plugin dependency to gradle notation and returns mapped provider`() {
        val provider = mockk<Provider<PluginDependency>>()
        val mappedProvider = mockk<Provider<String>>()
        val captured: CapturingSlot<Transformer<String, PluginDependency>> = slot()
        every { provider.map(capture(captured)) } returns mappedProvider

        val plugin = mockk<PluginDependency>()
        every { plugin.pluginId } returns "com.acme.plugin"
        every { plugin.version } returns mockk<VersionConstraint>().also {
            every { it.toString() } returns "1.2.3"
        }

        assertSame(mappedProvider, provider.asDependency())

        val notation = captured.captured.transform(plugin)
        assertEquals("com.acme.plugin:com.acme.plugin.gradle.plugin:1.2.3", notation)
    }
}
