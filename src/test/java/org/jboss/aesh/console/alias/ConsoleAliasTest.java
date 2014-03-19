/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.alias;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsoleAliasTest extends BaseConsoleTest {


    @Test
    public void alias() throws Exception {
        SettingsBuilder builder = new SettingsBuilder();
        builder.enableAlias(true);
        builder.persistAlias(false);
        builder.aliasFile(Config.isOSPOSIXCompatible() ?
                new File("src/test/resources/alias1") : new File("src\\test\\resources\\alias1"));

        invokeTestConsole(2, new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws IOException {
                out.write(("ll"+Config.getLineSeparator()).getBytes());
                out.flush();
                out.write(("grep -l"+Config.getLineSeparator()).getBytes());
            }
        }, new Verify() {
           private int count = 0;
           @Override
           public int call(Console console, ConsoleOperation op) {
               if(count == 0)
                   assertEquals("ls -alF", op.getBuffer());
               else if(count == 1) {
                   assertEquals("grep --color=auto -l", op.getBuffer());
               }
               count++;
               return 0;
           }
        }, builder);
    }
}
