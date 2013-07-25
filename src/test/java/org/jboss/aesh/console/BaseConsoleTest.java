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
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.TestTerminal;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class BaseConsoleTest {

    private Settings getDefaultSettings(InputStream is, SettingsBuilder builder) {
        if(builder == null) {
            builder = new SettingsBuilder();
            builder.enableAlias(false);
        }
        builder.readInputrc(false);
        builder.terminal(new TestTerminal());
        builder.inputStream(is);
        builder.outputStream(new ByteArrayOutputStream());
        builder.readAhead(false);

        if(!Config.isOSPOSIXCompatible())
            builder.ansi(false);

        builder.create().getOperationManager().addOperation(new KeyOperation(Key.ENTER, Operation.NEW_LINE));

        return builder.create();
    }

    public Console getTestConsole(SettingsBuilder builder, InputStream is) throws IOException {
        return new Console(getDefaultSettings(is, builder));
    }

    public Console getTestConsole(InputStream is) throws IOException {
        return new Console(getDefaultSettings(is, null));
    }

    public String getContentOfFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();

        }
        finally {
            br.close();
        }
    }

}
