package io.github.adelinosousa.gradle.extensions

import org.gradle.api.provider.Property

/**
 * Extension to configure GitHub CLI authentication.
 */
public interface GhCliAuthExtension {
    /**
     * The GitHub token to use for authentication.
     */
    public val token: Property<String?>
}