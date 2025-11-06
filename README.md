# GitHub CLI Auth Gradle Plugin

[![Kotlin](https://img.shields.io/badge/Kotlin-%237F52FF.svg?logo=kotlin&logoColor=white)](#)
[![Continuous Integration](https://github.com/adelinosousa/gh-cli-auth/actions/workflows/pr-checks.yml/badge.svg)](https://github.com/adelinosousa/gh-cli-auth/actions/workflows/pr-checks.yml)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.adelinosousa.gradle.plugins.settings.gh-cli-auth.svg?label=Settings%20Plugin)](https://plugins.gradle.org/plugin/io.github.adelinosousa.gradle.plugins.settings.gh-cli-auth)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.adelinosousa.gradle.plugins.project.gh-cli-auth.svg?label=Project%20Plugin)](https://plugins.gradle.org/plugin/io.github.adelinosousa.gradle.plugins.project.gh-cli-auth)

1. [Overview](#overview)
2. [Features](#features)
3. [Installation](#installation)
    - [A) Settings Plugin (Recommended)](#a-settings-plugin-recommended)
    - [B) Project Plugin (Per Project)](#b-project-plugin-per-project)
4. [Usage](#usage)
    - [1) Required: Set Your GitHub Organization](#1-required-tell-the-plugin-which-organization-to-use)
    - [2) Choose How to Provide Credentials](#2-choose-how-you-want-to-provide-credentials)
5. [Configuration Options](#configuration-options)
    - [Token Resolution Order](#token-resolution-order)
    - [GitHub CLI Scopes](#github-cli-scopes-cli-fallback)
    - [Repository Configuration](#repository-thats-registered)
6. [CI Tips](#ci-tips)
7. [Troubleshooting](#troubleshooting)
8. [Limitations](#limitations)
9. [Contributing](#contributing)
10. [License](#license)

## Overview

**Zero‑boilerplate access to GitHub Packages (Maven) for your organization.**

This plugin family configures the GitHub Packages Maven repository for your org and provides credentials automatically from one of three sources (in order):

1. **Environment variable** (name configurable, default `GITHUB_TOKEN`)
2. **Gradle property** (key configurable, default `gpr.token`)
3. **GitHub CLI**: parses `gh auth status --show-token` (requires `read:packages, read:org`)

> [!NOTE] 
> This allows you to onboard this plugin to existing production CI/CD pipelines with minimal changes, while also supporting local development via the GitHub CLI.

It works as a **settings** plugin (centralized repository management for the whole build) and/or a **project** plugin (per‑project repository + a `ghCliAuth` extension to read the token).

## Features

- Registers your authenticated GitHub Packages Maven repository for your organization automatically.
- Complete backwards compatibility with existing environment-based and Gradle property-based token provisioning.
- Ensures common “trusted” repos are present at settings level (added *only if missing*) such as Maven Central and Gradle Plugin Portal.
- Most importantly, No need to rely on hardcoded tokens local configs anymore, just use the GitHub CLI for local dev!

## Installation

You can use **either** plugin—or **both** together.

> [!TIP]
> **Recommendation:** In multi‑module builds (or when using `RepositoriesMode.FAIL_ON_PROJECT_REPOS`), prefer the **settings** plugin to centralize repository configuration. The **project** plugin declares repositories at project level and may conflict with `FAIL_ON_PROJECT_REPOS`.

### A) Settings plugin (recommended)

**Kotlin DSL – `settings.gradle.kts`**

```kotlin
plugins {
    id("io.github.adelinosousa.gradle.plugins.settings.gh-cli-auth") version "2.0.0"
}
```

**Groovy DSL – `settings.gradle`**

```groovy
plugins {
    id 'io.github.adelinosousa.gradle.plugins.settings.gh-cli-auth' version '2.0.0'
}
```

With the settings plugin applied, your build will have:

- **GitHub Packages** repo for your org in both `pluginManagement` and `dependencyResolutionManagement`.
- **Default** repos added if missing: Gradle Plugin Portal, Google, Maven Central.
- A shared token available at `gradle.extra["gh.cli.auth.token"]`.

### B) Project plugin (per project)

**Kotlin DSL – `build.gradle.kts`**

```kotlin
plugins {
    id("io.github.adelinosousa.gradle.plugins.project.gh-cli-auth") version "2.0.0"
}
```

**Groovy DSL – `build.gradle`**

```groovy
plugins {
    id 'io.github.adelinosousa.gradle.plugins.project.gh-cli-auth' version '2.0.0'
}
```

With the project plugin applied, your project will have:
- **GitHub Packages** repo for your org at `project.repositories`.
- The **`ghCliAuth`** extension exposing the token:
    - Kotlin: `val token: String? = extensions.getByName("ghCliAuth") as io.github.adelinosousa.gradle.extensions.GhCliAuthExtension; token.token.get()`
    - Groovy: `def token = extensions.getByName("ghCliAuth").token.get()`

## Usage

### 1) Required: Tell the plugin which **organization** to use

Add this to your **`gradle.properties`** (root of the build):

```properties
gh.cli.auth.github.org=<your-organization>
````

### 2) Choose how you want to provide credentials

You can do nothing (and rely on the GitHub CLI path below), or pick one of these:

- **Environment variable** (fastest for CI):
    - Leave default: export `GITHUB_TOKEN`
    - Or choose a name: set `gh.cli.auth.env.name` in `gradle.properties` and export that variable.

- **Gradle property** (CLI args or `gradle.properties`):
    - Leave default key: `gpr.token`
    - Or choose a key: set `gh.cli.auth.property.name` and pass `-P<that-key>=<token>` (or define it in `gradle.properties`).

- **GitHub CLI** fallback:
    - Make sure `gh` is installed and authenticated with the required scopes:

        ```bash
        gh auth login --scopes "read:packages,read:org"
        # or, if already logged in:
        gh auth refresh --scopes "read:packages,read:org"
        gh auth status
        ```

> [!WARNING]
> If both ENV and Gradle property are absent, the plugin automatically falls back to the GitHub CLI route.

## Configuration Options

| Key / Surface                       | Where to set/read                   | Default        | Purpose                                                                                                                        |
|-------------------------------------|-------------------------------------|----------------|--------------------------------------------------------------------------------------------------------------------------------|
| `gh.cli.auth.github.org`            | `gradle.properties`                 | **(required)** | GitHub Organization used to build the repo URL and name the repo entry (`https://maven.pkg.github.com/<org>/*`).               |
| `gh.cli.auth.env.name`              | `gradle.properties`                 | `GITHUB_TOKEN` | Name of the environment variable the plugin checks **first** for the token.                                                    |
| `gh.cli.auth.property.name`         | `gradle.properties`                 | `gpr.token`    | Name of the Gradle property the plugin checks **second** for the token (e.g., pass `-Pgpr.token=...` or define in properties). |
| `gradle.extra["gh.cli.auth.token"]` | **read** in `settings.gradle(.kts)` | n/a            | Token shared by the **settings** plugin for use by other settings logic/plugins.                                               |
| `ghCliAuth.token`                   | **read** in `build.gradle(.kts)`    | n/a            | Token exposed by the **project** plugin’s extension.                                                                           |
| `-Dgh.cli.binary.path=/path/to/gh`  | JVM/system property                 | auto‑detect    | Override the `gh` binary path used by the CLI fallback. Useful for custom installs (e.g., Homebrew prefix, Nix).               |

### Token resolution order

```
ENV (name = gh.cli.auth.env.name, default GITHUB_TOKEN)
  └── if unset/empty → GRADLE PROPERTY (key = gh.cli.auth.property.name, default gpr.token)
        └── if unset/empty → GitHub CLI: gh auth status --show-token
```

### GitHub CLI scopes (CLI fallback):

Below is the required scopes for the token retrieved via the GitHub CLI:

- `read:packages`
- `read:org`

If the token lacks these scopes, the plugin will fail with an error message prompting you to refresh your authentication.

### Repository that’s registered:

`https://maven.pkg.github.com/<org>/*` (name = `<org>`), with credentials automatically supplied by the selected token source.

> [!NOTE]
> Note on username: when the **CLI** path is used, the plugin extracts your GitHub login and uses it as the repository credential username; when **ENV/Gradle property** is used, the username is left empty.

## CI tips

- **GitHub Actions**: the default `GITHUB_TOKEN` environment variable is already present → no extra config needed; just set `gh.cli.auth.github.org`.
- **Local development**: Rely on the GitHub CLI route (make sure you’ve logged in with the correct scopes).

## Troubleshooting

- **“Please set `gh.cli.auth.github.org` in gradle.properties.”**  
  Add `gh.cli.auth.github.org=<your-org>` to `gradle.properties`.

- **“GitHub CLI token is missing required scopes …”**  
  Run:

    ```bash
    gh auth refresh --scopes "read:packages,read:org"
    gh auth status
    ```

- **Custom `gh` install not found**  
  Point the plugin at your binary:

    ```
    ./gradlew -Dgh.cli.binary.path=/absolute/path/to/gh <task>
    ```

- **Using `RepositoriesMode.FAIL_ON_PROJECT_REPOS`**  
  Prefer the **settings** plugin (the project plugin adds repositories at the project level and may conflict with this mode).

## Limitations

- Only **Maven** repositories are configured.
- GitHub **Enterprise**/custom hosts and CLI **profile selection** are not supported; the CLI path expects `github.com` default auth.

## Contributing

PRs and issues are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

This project is licensed under the AGPL-3.0 License - see the [LICENSE](LICENSE) for details.

___