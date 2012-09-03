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
package org.jboss.jreadline.console.alias;

import junit.framework.TestCase;
import org.jboss.jreadline.console.Config;

import java.io.File;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AliasManagerTest extends TestCase {

    public AliasManagerTest(String name) {
        super(name);
    }

    public void testParseAlias() throws Exception {
        AliasManager manager = new AliasManager(new File("foo"));

        assertNull(manager.parseAlias("alias foo2='bar -s -h'"));
        assertNull(manager.parseAlias("alias foo=bar"));
        assertNull(manager.parseAlias("alias foo3=bar --help"));

        String out = manager.parseAlias("alias foo");
        assertEquals("alias foo='bar'"+ Config.getLineSeparator(), out);
        out = manager.parseAlias("alias foo2");
        assertEquals("alias foo2='bar -s -h'"+ Config.getLineSeparator(), out);
        out = manager.parseAlias("alias foo3");
        assertEquals("alias foo3='bar --help'"+ Config.getLineSeparator(), out);
        out = manager.parseAlias("alias");
        StringBuilder sb = new StringBuilder();
        sb.append("alias foo='bar'"+ Config.getLineSeparator())
                .append("alias foo2='bar -s -h'"+ Config.getLineSeparator())
                .append("alias foo3='bar --help'"+ Config.getLineSeparator());
        assertEquals(sb.toString(), out);
    }
}
