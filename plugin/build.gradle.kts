plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.plugin.publish)
}

group = "io.github.adelinosousa"

version = System
    .getenv("GRADLE_PUBLISH_VERSION")
    ?: requireNotNull(project.property("gradle.publish.version"))

dependencies {
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.extensions.jvm)
    testImplementation(libs.kotest.assertions.core)
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

repositories {
    mavenCentral()
}

@Suppress("UnstableApiUsage")
testing.suites.register<JvmTestSuite>("functionalTest").configure {
    dependencies { implementation(gradleTestKit()) }
    targets { all { testTask.configure { useJUnitPlatform() } } }
    configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
    configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])
    tasks.named<Task>("check") { dependsOn(this@configure) }
    kotlin.target.compilations { named { it == this@configure.name }.configureEach { associateWith(getByName("main")) } }
}

/**
 * Enable strict explicit API mode for this project as this is a public plugin.
 */
kotlin.explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Strict

tasks.named<Test>("test") {
    /**
     * Byte-buddy-agent is a dependency of mockk, and it is not a serviceability tool.
     * So, we need to enable dynamic agent loading to hide the warning and make it work in future JDK versions.
     */
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    /**
     * > OpenJDK 64-Bit Server VM warning:
     * > Sharing is only supported for bootloader classes because bootstrap classpath has been appended
     * For further details, see: https://github.com/gradle/gradle/issues/19989 is resolved.
     */
    jvmArgs("-Xshare:off")
    useJUnitPlatform()
}

gradlePlugin {
    website = "https://gh-cli-auth.digibit.uk"
    vcsUrl = "https://github.com/adelinosousa/gh-cli-auth"
    plugins {
        register("ghCliAuthProject") {
            id = "io.github.adelinosousa.gradle.plugins.project.gh-cli-auth"
            implementationClass = "io.github.adelinosousa.gradle.plugins.GhCliAuthProjectPlugin"
            displayName = "Gradle GitHub CLI Auth Project Plugin"
            description = "Automatically configures access to GitHub Maven Packages for project using gh CLI. CI/CD friendly."
            tags.set(listOf("github", "packages", "maven", "repository"))
        }
        register("ghCliAuthSettings") {
            id = "io.github.adelinosousa.gradle.plugins.settings.gh-cli-auth"
            implementationClass = "io.github.adelinosousa.gradle.plugins.GhCliAuthSettingsPlugin"
            displayName = "Gradle GitHub CLI Auth Settings Plugin"
            description = "Automatically configures access to GitHub Maven Plugins for settings using gh CLI. CI/CD friendly."
            tags.set(listOf("github", "packages", "maven", "repository"))
        }
    }
}
