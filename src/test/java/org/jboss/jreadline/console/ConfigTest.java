/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.console;

import junit.framework.TestCase;
import org.jboss.jreadline.console.settings.Settings;
import org.jboss.jreadline.edit.KeyOperation;
import org.jboss.jreadline.edit.Mode;
import org.jboss.jreadline.edit.actions.Operation;

import java.io.File;
import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConfigTest extends TestCase {

    public ConfigTest(String name) {
        super(name);
    }


    public void testParseInputrc() throws IOException {
        Settings settings = Settings.getInstance();
        settings.resetToDefaults();
        settings.setInputrc(new File("src/test/resources/inputrc1"));

        Config.parseInputrc(settings);

        assertEquals(settings.getEditMode(), Mode.VI);

        assertEquals(settings.getBellStyle(), "visible");

        assertEquals(settings.getHistorySize(), 300);

        assertEquals(settings.isDisableCompletion(), true);

    }

    public void testParseInputrc2() throws IOException {
        Settings settings = Settings.getInstance();
        settings.resetToDefaults();
        settings.setInputrc(new File("src/test/resources/inputrc2"));

        Config.parseInputrc(settings);

        assertEquals(new KeyOperation(new int[]{27,91,68}, Operation.MOVE_NEXT_CHAR),
                settings.getOperationManager().findOperation(new int[]{27,91,68}));

        assertEquals(new KeyOperation(new int[]{27,91,66}, Operation.HISTORY_PREV),
                settings.getOperationManager().findOperation(new int[]{27,91,66}));

        assertEquals(new KeyOperation(new int[]{27,10}, Operation.MOVE_PREV_CHAR),
                settings.getOperationManager().findOperation(new int[]{27,10}));

        assertEquals(new KeyOperation(new int[]{1}, Operation.MOVE_NEXT_WORD),
                settings.getOperationManager().findOperation(new int[]{1}));
    }

    public void testParseProperties() throws IOException {
        System.setProperty("jreadline.terminal", "org.jboss.jreadline.terminal.TestTerminal");
        System.setProperty("jreadline.editmode", "vi");
        System.setProperty("jreadline.historypersistent", "false");
        System.setProperty("jreadline.historydisabled", "true");
        System.setProperty("jreadline.historysize", "42");
        System.setProperty("jreadline.logging", "false");
        System.setProperty("jreadline.disablecompletion", "true");

        Config.readRuntimeProperties(Settings.getInstance());

        assertEquals(Settings.getInstance().getTerminal().getClass().getName(), "org.jboss.jreadline.terminal.TestTerminal");

        assertEquals(Settings.getInstance().getEditMode(), Mode.VI);

        assertEquals(Settings.getInstance().isHistoryPersistent(), false);
        assertEquals(Settings.getInstance().isHistoryDisabled(), true);
        assertEquals(Settings.getInstance().getHistorySize(), 42);
        assertEquals(Settings.getInstance().isLogging(), false);
        assertEquals(Settings.getInstance().isDisableCompletion(), true);

        System.setProperty("jreadline.terminal", "");
        System.setProperty("jreadline.editmode", "");
        System.setProperty("jreadline.historypersistent", "");
        System.setProperty("jreadline.historydisabled", "");
        System.setProperty("jreadline.historysize", "");
        System.setProperty("jreadline.logging", "");
        System.setProperty("jreadline.disablecompletion", "");

        Settings.getInstance().resetToDefaults();
    }
}
