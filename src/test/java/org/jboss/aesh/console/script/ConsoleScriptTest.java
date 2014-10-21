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
