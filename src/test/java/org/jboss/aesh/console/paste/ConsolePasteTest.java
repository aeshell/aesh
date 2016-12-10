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
package org.jboss.aesh.console.paste;

import static org.junit.Assert.assertEquals;

import org.aesh.readline.Prompt;
import org.aesh.util.Config;
import org.jboss.aesh.console.BaseConsoleTest;
import org.junit.Test;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ConsolePasteTest extends BaseConsoleTest {

    @Test
    public void paste() throws Exception {
        invokeTestConsole(4, new Setup() {
            @Override
            public void call(ConsoleInteraction console, Consumer<String> out) throws IOException {
                String pasteLine1 =
                        "connect" + Config.getLineSeparator() +
                        "admin" + Config.getLineSeparator() +
                        "admin!";
                String pasteLine2 = "234"+ Config.getLineSeparator() + "exit"+ Config.getLineSeparator();
                out.accept(pasteLine1);
                out.accept(pasteLine2);
            }
        }, new Verify() {
           boolean password = false;
           @Override
           public int call(ConsoleInteraction console, Supplier<String> op) {
               if (op.get().equals("admin")) {
                   console.setPrompt(new Prompt("", new Character('\u0000')));
                   password = true;
                   return 0;
               }
               if(password) {
                   assertEquals("admin!234", op.get());
                   password = false;
               }
               return 0;
           }
        });
    }
}
