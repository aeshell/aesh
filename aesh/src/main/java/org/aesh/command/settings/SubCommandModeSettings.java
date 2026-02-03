/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.settings;

/**
 * Configuration options for sub-command mode behavior.
 * Sub-command mode allows users to enter an interactive context for group commands
 * where subsequent commands are executed within that context.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface SubCommandModeSettings {

    /**
     * Check if sub-command mode is enabled globally.
     * When disabled, group commands will not enter interactive sub-command mode.
     *
     * @return true if sub-command mode is enabled (default: true)
     */
    boolean isEnabled();

    /**
     * Get the primary command to exit sub-command mode.
     *
     * @return the exit command (default: "exit")
     */
    String getExitCommand();

    /**
     * Get the alternative command to exit sub-command mode.
     * Set to null to disable the alternative exit command.
     *
     * @return the alternative exit command (default: "..")
     */
    String getAlternativeExitCommand();

    /**
     * Get the separator used for nested context paths in the prompt.
     * For example, with separator ":", nested contexts appear as "module:project>"
     *
     * @return the context separator (default: ":")
     */
    String getContextSeparator();

    /**
     * Check if option/argument values should be displayed when entering sub-command mode.
     *
     * @return true to show context values on entry (default: true)
     */
    boolean showContextOnEntry();

    /**
     * Check if the primary argument/option value should be shown in the prompt.
     * For example, "project[myapp]>" vs "project>"
     *
     * @return true to show argument in prompt (default: true)
     */
    boolean showArgumentInPrompt();

    /**
     * Get the command name to display/redisplay current context values.
     * Set to null to disable the context command.
     *
     * @return the context command name (default: "context")
     */
    String getContextCommand();

    /**
     * Get the message format shown when entering sub-command mode.
     * Supports placeholders: {name} for command name.
     *
     * @return the enter message format (default: "Entering {name} mode.")
     */
    String getEnterMessage();

    /**
     * Get the message format shown when exiting sub-command mode.
     * Supports placeholders: {name} for command name.
     *
     * @return the exit message format (default: null - no message)
     */
    String getExitMessage();

    /**
     * Get the format for the exit hint message.
     * Supports placeholders: {exit} for exit command, {alt} for alternative exit.
     *
     * @return the exit hint format (default: "Type '{exit}' to return.")
     */
    String getExitHint();

    /**
     * Check if Ctrl+C should exit sub-command mode instead of interrupting.
     *
     * @return true if Ctrl+C exits sub-command mode (default: true)
     */
    boolean exitOnCtrlC();

    /**
     * Get the default settings instance with all default values.
     *
     * @return default SubCommandModeSettings
     */
    static SubCommandModeSettings defaults() {
        return DefaultSubCommandModeSettings.INSTANCE;
    }

    /**
     * Create a new builder for SubCommandModeSettings.
     *
     * @return a new builder
     */
    static SubCommandModeSettingsBuilder builder() {
        return new SubCommandModeSettingsBuilder();
    }
}
