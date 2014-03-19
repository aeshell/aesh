/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.edit;

import static org.junit.Assert.assertEquals;

import java.io.OutputStream;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.Mode;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ViEditingTest extends BaseConsoleTest {

    @Test
    public void testVi() throws Exception {
        SettingsBuilder builder = new SettingsBuilder();
        builder.mode(Mode.VI);

        invokeTestConsole(1, new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws Exception {
                out.write("34".getBytes());
                //esc
                out.write(new byte[]{27});
                //ctrl-e (should switch to emacs mode)
                out.write(new byte[]{5});
                //ctrl-a
                out.write(new byte[]{1});
                out.write(("12"+Config.getLineSeparator()).getBytes());
                out.flush();
            }
        }, new Verify() {
           @Override
           public int call(Console console, ConsoleOperation op) {
               assertEquals("1234", op.getBuffer());
               return 0;
           }
        }, builder);
    }
}
