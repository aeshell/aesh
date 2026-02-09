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
package org.aesh.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.settings.RuntimeSettings;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.readline.editing.EditMode;
import org.aesh.terminal.utils.Config;
import org.junit.Test;

/**
 * @author Aesh team
 */
public class ConfigTest {

    @Test
    public void testParseProperties() {
        System.setProperty("aesh.terminal", "org.jboss.aesh.terminal.TestTerminal");
        System.setProperty("aesh.editmode", "vi");
        System.setProperty("aesh.historypersistent", "false");
        System.setProperty("aesh.historydisabled", "true");
        System.setProperty("aesh.historysize", "42");
        System.setProperty("aesh.logging", "false");
        System.setProperty("aesh.disablecompletion", "true");
        System.setProperty("aesh.execute", "foo -f --bar");

        SettingsBuilder<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation, OptionActivator, CommandActivator> builder = SettingsBuilder
                .builder();
        Settings settings = RuntimeSettings.readRuntimeProperties(builder.build());

        assertEquals(settings.mode(), EditMode.Mode.VI);

        assertFalse(settings.historyPersistent());
        assertTrue(settings.historyDisabled());
        assertEquals(settings.historySize(), 42);
        assertFalse(settings.logging());
        assertTrue(settings.completionDisabled());

        assertEquals(settings.executeAtStart(), "foo -f --bar" + Config.getLineSeparator());

        System.setProperty("aesh.terminal", "");
        System.setProperty("aesh.editmode", "");
        System.setProperty("aesh.historypersistent", "");
        System.setProperty("aesh.historydisabled", "");
        System.setProperty("aesh.historysize", "");
        System.setProperty("aesh.logging", "");
        System.setProperty("aesh.disablecompletion", "");
        System.setProperty("aesh.execute", "");

        builder = SettingsBuilder.builder();
        settings = RuntimeSettings.readRuntimeProperties(builder.build());

        assertNull(settings.executeAtStart());
    }
}
