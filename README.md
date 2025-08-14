[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.adelinosousa.gradle.plugins.settings.gh-cli-auth.svg?label=gh-cli-auth%20Settings%20Plugin)](https://plugins.gradle.org/plugin/io.github.adelinosousa.gradle.plugins.settings.gh-cli-auth)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.adelinosousa.gradle.plugins.project.gh-cli-auth.svg?label=gh-cli-auth%20Project%20Plugin)](https://plugins.gradle.org/plugin/io.github.adelinosousa.gradle.plugins.project.gh-cli-auth)

# GitHub CLI Auth Gradle Plugin

Gradle plugin that automatically configures access to GitHub organization maven plugins and packages. Authenticates using credentials from GitHub CLI and removes the need to store personal access tokens (PATs) in your project, environment or gradle configuration.

## Prerequisites

You need to have [GitHub CLI](https://cli.github.com/) installed on your system and be logged in to your GitHub account:

```bash
gh auth login --scopes "read:packages,repo,read:org"
```

If you're already logged in but don't have the required scopes, you can refresh your authentication using:

```bash
gh auth refresh --scopes "read:packages,repo,read:org"
```

To check your current GitHub CLI authentication status, do:

```bash
gh auth status
```

## Usage

This plugin is split into two: one for `settings` and the other for `project`. Depending on your solution repository management, you can choose to use either one.

1. Setup for `settings`:

   In your `settings.gradle` file, add the following:

   ```shell
   # settings.gradle
   
   # to ensure that the repositories are resolved from settings.gradle
   dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    }

   plugins {
       id 'io.github.adelinosousa.gradle.plugins.settings.gh-cli-auth' version '1.0.1'
   }
   ```

2. Setup for `project`:

   In your `build.gradle` file, add the following:

   ```shell
   # build.gradle

   plugins {
       id 'io.github.adelinosousa.gradle.plugins.project.gh-cli-auth' version '1.0.1'
   }
   ```

   This plugin also exposes a `ghCliAuth` extension to access the token, if needed:

   ```shell
   # build.gradle

   val ghToken = ghCliAuth.token.get()
   ```

**NOTE**: When using both plugins, ensure that you **only** apply the plugin version to <u>settings</u> plugin block and not to the <u>project</u> plugin block, as it will lead to a conflict.

### Configuration

Regardless of which one you use, you need to specify your GitHub **organization**, where the plugins or packages are hosted, in the `gradle.properties` file:

```properties
# gradle.properties

gh.cli.auth.github.org=<your-organization>
```

You can also specify custom environment variable name for the GitHub CLI authentication token. Defaults `GITHUB_TOKEN`.

```properties
# gradle.properties

gh.cli.auth.env.name=<environment-variable-name>
```

**NOTE**: Environment variable takes precedence over the GitHub CLI token mechanism. GitHub CLI is used as a fallback if the environment variable is not set. 
This is by design, to ensure that the plugin remains performant and skips unnecessary checks/steps during CI/CD runs.

## Notes

Currently **not** supported:

- Dedicated GitHub Enterprise Servers or Hosts
- Profile selection (the plugin uses the default from `gh`)
- Only Maven repositories are supported (no Ivy or other types)

## Contributing
Contributions are welcome! Please read the contributing [guidelines](CONTRIBUTING.md).

## License
This project is licensed under the AGPL-3.0 License - see the [LICENSE](LICENSE) for details.