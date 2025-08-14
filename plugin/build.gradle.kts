plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.jvm)
    id("com.gradle.plugin-publish") version "1.3.1"
}

group = "io.github.adelinosousa"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.mockk)
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    website = "https://gh-cli-auth.linos.dev"
    vcsUrl = "https://github.com/adelinosousa/gh-cli-auth"
    plugins {
        create("ghCliAuthProject") {
            id = "io.github.adelinosousa.gradle.plugins.project.gh-cli-auth"
            implementationClass = "io.github.adelinosousa.gradle.plugins.GhCliAuthProjectPlugin"
            displayName = "Gradle GitHub CLI Auth Project Plugin"
            description =
                "Automatically configures access to GitHub Maven Packages for project using gh CLI. CI/CD friendly."
            tags.set(listOf("github", "packages", "maven", "repository"))
        }
        create("ghCliAuthSettings") {
            id = "io.github.adelinosousa.gradle.plugins.settings.gh-cli-auth"
            implementationClass = "io.github.adelinosousa.gradle.plugins.GhCliAuthSettingsPlugin"
            displayName = "Gradle GitHub CLI Auth Settings Plugin"
            description =
                "Automatically configures access to GitHub Maven Plugins for settings using gh CLI. CI/CD friendly."
            tags.set(listOf("github", "packages", "maven", "repository"))
        }
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
