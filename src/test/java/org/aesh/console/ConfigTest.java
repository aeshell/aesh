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
package org.aesh.console;

import org.aesh.console.settings.RuntimeSettings;
import org.aesh.console.settings.Settings;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.readline.editing.EditMode;
import org.aesh.utils.Config;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConfigTest {

    @Test
    public void testParseProperties() throws IOException {
        System.setProperty("aesh.terminal", "org.jboss.aesh.terminal.TestTerminal");
        System.setProperty("aesh.editmode", "vi");
        System.setProperty("aesh.historypersistent", "false");
        System.setProperty("aesh.historydisabled", "true");
        System.setProperty("aesh.historysize", "42");
        System.setProperty("aesh.logging", "false");
        System.setProperty("aesh.disablecompletion", "true");
        System.setProperty("aesh.execute", "foo -f --bar");

        SettingsBuilder builder = SettingsBuilder.builder();
        Settings settings = RuntimeSettings.readRuntimeProperties(builder.build());

        assertEquals(settings.mode(), EditMode.Mode.VI);

        assertEquals(settings.historyPersistent(), false);
        assertEquals(settings.historyDisabled(), true);
        assertEquals(settings.historySize(), 42);
        assertEquals(settings.logging(), false);
        assertEquals(settings.completionDisabled(), true);

        assertEquals(settings.executeAtStart(), "foo -f --bar"+ Config.getLineSeparator());

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

        assertEquals(settings.executeAtStart(), null);
    }
}
