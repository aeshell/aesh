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
package org.jboss.aesh.readline.editing;

import static org.junit.Assert.assertEquals;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.readline.editing.EditMode;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Ignore
public class ViEditingTest extends BaseConsoleTest {

    @Test
    public void testVi() throws Exception {
        SettingsBuilder builder = new SettingsBuilder();
        builder.mode(EditMode.Mode.VI);

        invokeTestConsole(1, (console, out) -> {
            out.write("34".getBytes());
            //esc
            out.write(new byte[]{27});
            //ctrl-e (should switch to emacs mode)
            out.write(new byte[]{5});
            //ctrl-a
            out.write(new byte[]{1});
            out.write(("12"+Config.getLineSeparator()).getBytes());
            out.flush();
        }, (console, op) -> {
            assertEquals("1234", op.getBuffer());
            return 0;
        }, builder);
    }
}
