/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.jreadline;

import junit.framework.TestCase;
import org.jboss.jreadline.console.Console;
import org.jboss.jreadline.console.settings.Settings;
import org.jboss.jreadline.edit.Mode;

import java.io.*;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class JReadlineTestCase extends TestCase {

    public JReadlineTestCase(String test) {
        super(test);
    }

    public void assertEquals(String expected, TestBuffer buffer) throws IOException {

        Settings settings = Settings.getInstance();
        settings.setReadInputrc(false);
        settings.setInputStream(new ByteArrayInputStream(buffer.getBytes()));
        settings.setOutputStream(new ByteArrayOutputStream());
        settings.setEditMode(Mode.EMACS);
        Console console = new Console(settings);


        String in = null;
        while (true) {
            String tmp = console.read(null);
            if(tmp != null)
                in = tmp;
            else
                break;
        }
        try {
            console.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals(expected, in);
    }

    public void assertEqualsViMode(String expected, TestBuffer buffer) throws IOException {

        Settings settings = Settings.getInstance();
        settings.setReadInputrc(false);
        settings.setInputStream(new ByteArrayInputStream(buffer.getBytes()));
        settings.setOutputStream(new ByteArrayOutputStream());
        settings.setEditMode(Mode.VI);

        Console console = new Console(settings);

        String in = null;
        while (true) {
            String tmp = console.read(null);
            if(tmp != null)
                in = tmp;
            else
                break;
        }
        try {
            console.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals(expected, in);
    }
}
