/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.KeyOperation;
import org.jboss.aesh.edit.Mode;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConfigTest {

    @Test
    public void testParseInputrc() throws IOException {
        SettingsBuilder builder = new SettingsBuilder();
        builder.inputrc( Config.isOSPOSIXCompatible() ?
                new File("src/test/resources/inputrc1") : new File("src\\test\\resources\\inputrc1"));

        Settings settings = Config.parseInputrc(builder.create());

        assertEquals(settings.getMode(), Mode.VI);

        assertEquals(settings.getBellStyle(), "visible");

        assertEquals(settings.getHistorySize(), 300);

        assertEquals(settings.isDisableCompletion(), true);

    }

    @Test
    public void testParseInputrc2() throws IOException {
        SettingsBuilder builder = new SettingsBuilder();
        builder.inputrc( Config.isOSPOSIXCompatible() ?
                new File("src/test/resources/inputrc2") : new File("src\\test\\resources\\inputrc2"));

        if(Config.isOSPOSIXCompatible()) {  //TODO: must fix this for windows
            Settings settings = Config.parseInputrc(builder.create());

            assertEquals(new KeyOperation(Key.LEFT, Operation.MOVE_NEXT_CHAR),
                    settings.getOperationManager().findOperation(new int[]{27,91,68}));

            assertEquals(new KeyOperation(Key.DOWN, Operation.HISTORY_PREV),
                    settings.getOperationManager().findOperation(new int[]{27,91,66}));

            assertEquals(new KeyOperation(Key.META_CTRL_J, Operation.MOVE_PREV_CHAR),
                    settings.getOperationManager().findOperation(new int[]{27,10}));

            assertEquals(new KeyOperation(Key.CTRL_A, Operation.MOVE_NEXT_WORD),
                    settings.getOperationManager().findOperation(new int[]{1}));
        }
    }

    @Test
    public void testParseProperties() throws IOException {
        System.setProperty("aesh.terminal", "org.jboss.aesh.terminal.TestTerminal");
        System.setProperty("aesh.editmode", "vi");
        System.setProperty("aesh.historypersistent", "false");
        System.setProperty("aesh.historydisabled", "true");
        System.setProperty("aesh.historysize", "42");
        System.setProperty("aesh.logging", "false");
        System.setProperty("aesh.disablecompletion", "true");

        SettingsBuilder builder = new SettingsBuilder();
        Settings settings = Config.readRuntimeProperties(builder.create());

        assertEquals(settings.getTerminal().getClass().getName(), "org.jboss.aesh.terminal.TestTerminal");

        assertEquals(settings.getMode(), Mode.VI);

        assertEquals(settings.isHistoryPersistent(), false);
        assertEquals(settings.isHistoryDisabled(), true);
        assertEquals(settings.getHistorySize(), 42);
        assertEquals(settings.isLogging(), false);
        assertEquals(settings.isDisableCompletion(), true);

        System.setProperty("aesh.terminal", "");
        System.setProperty("aesh.editmode", "");
        System.setProperty("aesh.historypersistent", "");
        System.setProperty("aesh.historydisabled", "");
        System.setProperty("aesh.historysize", "");
        System.setProperty("aesh.logging", "");
        System.setProperty("aesh.disablecompletion", "");
    }
}
