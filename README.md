# GitHub CLI Auth Gradle Plugin

Gradle plugin that automatically configures access to GitHub organization plugins and packages. Authenticates by using credentials from the official GitHub CLI (gh), removing the need to store personal access tokens (PATs) in your project or gradle configuration.

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

This plugin is split into two: one for `plugins` and the other for `repositories`. Depending on what you need to set up, you can use either or both.

1. Setup for `plugins`:

   In your `settings.gradle` file, add the following:

   ```shell
   # settings.gradle

   plugins {
       id 'dev.linos.gradle.plugins.settings.gh-cli-auth' version '1.0.0'
   }
   ```

2. Setup for `repositories`:

   In your `build.gradle` file, add the following:

   ```shell
   # build.gradle

   plugins {
       id 'dev.linos.gradle.plugins.project.gh-cli-auth' version '1.0.0'
   }
   ```

   This plugin also exposes a `ghCliAuth` extension to access the token, if needed:

   ```shell
   # build.gradle

   val ghToken = ghCliAuth.token.get()
   ```

**NOTE**: When using both plugins, ensure that you only apply the plugin version to settings plugin block and not to the project plugin block, as it will lead to a conflict.

### Configuration

Regardless of which one you use, you need to specify your GitHub **organization**, where the plugins or packages are hosted, in the `gradle.properties` file:

```properties
# gradle.properties

gh.cli.auth.github.org=<your-organization>
```

You can also specify an **environment** variable as a fallback mechanism (for example, in CI/CD environments):

```properties
# gradle.properties

gh.cli.auth.env.name=<environment-variable-name>
```

## Notes

Currently not supported features:

- Dedicated GitHub Enterprise Servers or Hosts
- Profile selection (the plugin uses the default from `gh`)