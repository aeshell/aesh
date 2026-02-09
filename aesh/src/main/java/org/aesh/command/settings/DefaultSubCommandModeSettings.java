/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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
 * Default implementation of SubCommandModeSettings with sensible defaults.
 *
 * @author Aesh team
 */
class DefaultSubCommandModeSettings implements SubCommandModeSettings {

    static final DefaultSubCommandModeSettings INSTANCE = new DefaultSubCommandModeSettings();

    private final boolean enabled;
    private final String exitCommand;
    private final String alternativeExitCommand;
    private final String contextSeparator;
    private final boolean showContextOnEntry;
    private final boolean showArgumentInPrompt;
    private final String contextCommand;
    private final String enterMessage;
    private final String exitMessage;
    private final String exitHint;
    private final boolean exitOnCtrlC;

    /**
     * Create default settings.
     */
    DefaultSubCommandModeSettings() {
        this.enabled = true;
        this.exitCommand = "exit";
        this.alternativeExitCommand = "..";
        this.contextSeparator = ":";
        this.showContextOnEntry = true;
        this.showArgumentInPrompt = true;
        this.contextCommand = "context";
        this.enterMessage = "Entering {name} mode.";
        this.exitMessage = null;
        this.exitHint = "Type '{exit}' to return.";
        this.exitOnCtrlC = true;
    }

    /**
     * Create settings with specified values.
     */
    DefaultSubCommandModeSettings(boolean enabled, String exitCommand, String alternativeExitCommand,
            String contextSeparator, boolean showContextOnEntry,
            boolean showArgumentInPrompt, String contextCommand,
            String enterMessage, String exitMessage, String exitHint,
            boolean exitOnCtrlC) {
        this.enabled = enabled;
        this.exitCommand = exitCommand;
        this.alternativeExitCommand = alternativeExitCommand;
        this.contextSeparator = contextSeparator;
        this.showContextOnEntry = showContextOnEntry;
        this.showArgumentInPrompt = showArgumentInPrompt;
        this.contextCommand = contextCommand;
        this.enterMessage = enterMessage;
        this.exitMessage = exitMessage;
        this.exitHint = exitHint;
        this.exitOnCtrlC = exitOnCtrlC;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getExitCommand() {
        return exitCommand;
    }

    @Override
    public String getAlternativeExitCommand() {
        return alternativeExitCommand;
    }

    @Override
    public String getContextSeparator() {
        return contextSeparator;
    }

    @Override
    public boolean showContextOnEntry() {
        return showContextOnEntry;
    }

    @Override
    public boolean showArgumentInPrompt() {
        return showArgumentInPrompt;
    }

    @Override
    public String getContextCommand() {
        return contextCommand;
    }

    @Override
    public String getEnterMessage() {
        return enterMessage;
    }

    @Override
    public String getExitMessage() {
        return exitMessage;
    }

    @Override
    public String getExitHint() {
        return exitHint;
    }

    @Override
    public boolean exitOnCtrlC() {
        return exitOnCtrlC;
    }
}
