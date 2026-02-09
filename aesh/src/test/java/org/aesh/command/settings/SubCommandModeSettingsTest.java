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

import static org.junit.Assert.*;

import org.aesh.command.impl.context.CommandContext;
import org.junit.Test;

/**
 * Tests for SubCommandModeSettings.
 */
public class SubCommandModeSettingsTest {

    @Test
    public void testDefaultSettings() {
        SubCommandModeSettings settings = SubCommandModeSettings.defaults();

        assertTrue(settings.isEnabled());
        assertEquals("exit", settings.getExitCommand());
        assertEquals("..", settings.getAlternativeExitCommand());
        assertEquals(":", settings.getContextSeparator());
        assertTrue(settings.showContextOnEntry());
        assertTrue(settings.showArgumentInPrompt());
        assertEquals("context", settings.getContextCommand());
        assertEquals("Entering {name} mode.", settings.getEnterMessage());
        assertNull(settings.getExitMessage());
        assertEquals("Type '{exit}' to return.", settings.getExitHint());
        assertTrue(settings.exitOnCtrlC());
    }

    @Test
    public void testBuilder() {
        SubCommandModeSettings settings = SubCommandModeSettings.builder()
                .enabled(false)
                .exitCommand("quit")
                .alternativeExitCommand("back")
                .contextSeparator("/")
                .showContextOnEntry(false)
                .showArgumentInPrompt(false)
                .contextCommand(null)
                .enterMessage("Entering {name}...")
                .exitMessage("Leaving {name}.")
                .exitHint("Use '{exit}' or '{alt}' to exit.")
                .exitOnCtrlC(false)
                .build();

        assertFalse(settings.isEnabled());
        assertEquals("quit", settings.getExitCommand());
        assertEquals("back", settings.getAlternativeExitCommand());
        assertEquals("/", settings.getContextSeparator());
        assertFalse(settings.showContextOnEntry());
        assertFalse(settings.showArgumentInPrompt());
        assertNull(settings.getContextCommand());
        assertEquals("Entering {name}...", settings.getEnterMessage());
        assertEquals("Leaving {name}.", settings.getExitMessage());
        assertEquals("Use '{exit}' or '{alt}' to exit.", settings.getExitHint());
        assertFalse(settings.exitOnCtrlC());
    }

    @Test
    public void testCommandContextWithSettings() {
        SubCommandModeSettings settings = SubCommandModeSettings.builder()
                .exitCommand("quit")
                .alternativeExitCommand("back")
                .contextSeparator("/")
                .build();

        CommandContext ctx = new CommandContext("test> ", settings);

        // Test exit command detection
        assertTrue(ctx.isExitCommand("quit"));
        assertTrue(ctx.isExitCommand("back"));
        assertFalse(ctx.isExitCommand("exit"));
        assertFalse(ctx.isExitCommand(""));
        assertFalse(ctx.isExitCommand(null));
    }

    @Test
    public void testMessageFormatting() {
        SubCommandModeSettings settings = SubCommandModeSettings.builder()
                .enterMessage("Welcome to {name}!")
                .exitMessage("Goodbye from {name}.")
                .exitHint("Press '{exit}' or '{alt}' to leave.")
                .exitCommand("quit")
                .alternativeExitCommand("back")
                .build();

        CommandContext ctx = new CommandContext("test> ", settings);

        assertEquals("Welcome to project!", ctx.formatEnterMessage("project"));
        assertEquals("Goodbye from project.", ctx.formatExitMessage("project"));
        assertEquals("Press 'quit' or 'back' to leave.", ctx.formatExitHint());
    }

    @Test
    public void testSettingsIntegrationWithMainSettings() {
        SubCommandModeSettings subSettings = SubCommandModeSettings.builder()
                .exitCommand("bye")
                .build();

        Settings<?, ?, ?, ?, ?, ?> settings = SettingsBuilder.builder()
                .subCommandModeSettings(subSettings)
                .build();

        assertEquals("bye", settings.subCommandModeSettings().getExitCommand());
    }

    @Test
    public void testDefaultSettingsFromMainSettings() {
        Settings<?, ?, ?, ?, ?, ?> settings = SettingsBuilder.builder().build();

        // Should return default settings
        assertNotNull(settings.subCommandModeSettings());
        assertTrue(settings.subCommandModeSettings().isEnabled());
        assertEquals("exit", settings.subCommandModeSettings().getExitCommand());
    }
}
