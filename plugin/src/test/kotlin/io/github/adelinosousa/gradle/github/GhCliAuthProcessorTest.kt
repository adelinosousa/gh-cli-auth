package io.github.adelinosousa.gradle.github

import io.github.adelinosousa.gradle.github.GhCliAuthProcessor.Companion.GH_CLI_BINARY_PATH
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.OutputStream
import org.gradle.api.Action
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec

import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import kotlin.test.Test

class GhCliAuthProcessorTest {
    private val originalOs = System.getProperty("os.name")
    private val originalGhPath = System.getProperty(GH_CLI_BINARY_PATH)

    @AfterEach
    fun restoreProps() {
        if (originalOs == null) System.clearProperty("os.name") else System.setProperty("os.name", originalOs)
        if (originalGhPath == null) System.clearProperty(GH_CLI_BINARY_PATH)
        else System.setProperty(GH_CLI_BINARY_PATH, originalGhPath)
    }

    @Test
    fun `obtain returns trimmed stdout on success and uses expected args`() {
        System.setProperty("os.name", "Linux")
        System.clearProperty(GH_CLI_BINARY_PATH)

        val captured = mutableListOf<Any?>()
        val execOps = mockExec("  some-output\n", captured)
        val processor = processorWith(execOps)

        processor.obtain() shouldBe "some-output"

        // Command and args irrespective of which overload Gradle used
        captured.first() shouldBe "gh"
        captured.drop(1) shouldBe listOf("auth", "status", "--show-token")

        verify { execOps.exec(any()) } // executed once
    }

    @Test
    fun `respects custom GH_CLI_BINARY_PATH`() {
        System.setProperty("os.name", "Linux")
        val custom = "/tmp/custom/gh"
        System.setProperty(GH_CLI_BINARY_PATH, custom)

        val captured = mutableListOf<Any?>()
        val execOps = mockExec("ok", captured)
        val processor = processorWith(execOps)

        processor.obtain() shouldBe "ok"
        captured.first() shouldBe custom
    }

    @Test
    fun `detects Windows exe`() {
        System.setProperty("os.name", "Windows 11")
        System.clearProperty(GH_CLI_BINARY_PATH)

        val captured = mutableListOf<Any?>()
        val execOps = mockExec("ok", captured)
        val processor = processorWith(execOps)

        processor.obtain() shouldBe "ok"
        captured.first() shouldBe "gh.exe"
    }

    @Test
    fun `wraps failures with helpful error if gh fails`() {
        System.setProperty("os.name", "Linux")
        System.clearProperty(GH_CLI_BINARY_PATH)

        // Simulate non-zero exit via assertNormalExitValue throwing
        val execOps = mockExec(stdout = "", captureCmd = mutableListOf(), okExit = false)
        val processor = processorWith(execOps)

        val ex = shouldThrow<IllegalStateException> { processor.obtain() }
        ex.message.shouldContain("Failed to authenticate:")
        ex.cause?.message shouldBe "non-zero exit"
    }

    /**
     * Creates an ExecOperations mock that returns [stdout] and captures the command line.
     *
     * @param stdout The standard output to simulate from the command.
     * @param captureCmd A mutable list that will be populated with the command line arguments used
     *                   when the exec is invoked.
     * @param okExit If true, simulates a successful exit; if false, simulates a non-zero exit.
     * @return A mocked ExecOperations instance.
     */
    private fun mockExec(
        stdout: String = "",
        captureCmd: MutableList<Any?> = mutableListOf(),
        okExit: Boolean = true,
    ): ExecOperations {
        val execOps = mockk<ExecOperations>()
        val execSpec = mockk<ExecSpec>(relaxed = true)
        val result = mockk<ExecResult>()
        val outSlot = slot<OutputStream>()

        // Capture BOTH overloads to be robust to Gradle's call site
        every { execSpec.commandLine(*anyVararg<Any>()) } answers {
            captureCmd.clear()
            @Suppress("UNCHECKED_CAST")
            val arr = this.args[0] as Array<Any?>
            captureCmd.addAll(arr.toList())
            execSpec
        }
        every { execSpec.commandLine(any<List<Any>>()) } answers {
            captureCmd.clear()
            captureCmd.addAll(firstArg())
            execSpec
        }

        // Capture setters used by the code under test
        every { execSpec.setStandardOutput(capture(outSlot)) } answers { execSpec }
        every { execSpec.setIgnoreExitValue(any()) } answers { execSpec }

        if (okExit) {
            every { result.assertNormalExitValue() } returns result
        } else {
            every { result.assertNormalExitValue() } throws RuntimeException("non-zero exit")
        }

        // Make exec(Action) run the action against our execSpec and write stdout
        every { execOps.exec(any()) } answers {
            val action = firstArg<Action<ExecSpec>>()
            action.execute(execSpec)
            if (outSlot.isCaptured) {
                outSlot.captured.write(stdout.toByteArray())
                outSlot.captured.flush()
            }
            result
        }

        return execOps
    }

    /** Convenience to construct a testable processor with our mocked ExecOperations. */
    private fun processorWith(execOps: ExecOperations) = object : GhCliAuthProcessor() {
        override val execOperations: ExecOperations = execOps
        override fun getParameters() = throw NotImplementedError() // Gradle won't call in these unit tests
    }
}
