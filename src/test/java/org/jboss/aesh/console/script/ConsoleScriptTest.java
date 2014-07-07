/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.script;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsoleScriptTest extends BaseConsoleTest {

    private List<String> lines;

    @Before
    public void readFile() throws IOException {
                lines = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader("src/test/resources/script1"));
        String line = br.readLine();
        while (line != null) {
            if (line.trim().length() > 0 && !line.trim().startsWith("#"))
                lines.add(line);
            line = br.readLine();
        }
    }

    @Test
    public void testScript() throws Throwable {

        invokeTestConsole(new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws Exception {
                out.write(("run"+Config.getLineSeparator()).getBytes());
                out.flush();

                Thread.sleep(1000);
            }
        }, new ScriptCallback());
    }

    class ScriptCallback implements Verify {

        private int count = 0;

        @Override
        public int call(Console console, ConsoleOperation output) throws InterruptedException {
            if(output.getBuffer().equals("run")) {
                count++;
                //run script
                for(String line : lines ) {
                    console.pushToInputStream(line);
                    if(!line.endsWith(Config.getLineSeparator()))
                        console.pushToInputStream(Config.getLineSeparator());
                }
            }
            else {
                if(count == 1 || count == 2) {
                    assertEquals("foo", output.getBuffer());
                    count++;
                }
                else if(count == 3) {
                    assertEquals("exit", output.getBuffer());
                }
            }
            return 0;
        }
    }
}
