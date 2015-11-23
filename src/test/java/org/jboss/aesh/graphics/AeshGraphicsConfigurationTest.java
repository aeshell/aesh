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
package org.jboss.aesh.graphics;

import org.jboss.aesh.terminal.api.Size;
import org.jboss.aesh.terminal.impl.LineDisciplineTerminal;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author <a href="mailto:00hf11@gmail.com">Helio Frota</a>
 */
public class AeshGraphicsConfigurationTest {

    @Test
    public void testAeshGraphicsConfiguration() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal = new LineDisciplineTerminal("test", "ansi", baos, "UTF-8");
        terminal.setSize(new Size(80, 20));
        AeshGraphicsConfiguration agc = new AeshGraphicsConfiguration(terminal);

        Assert.assertEquals("TerminalSize{height=80, width=20}", agc.getBounds().toString());
        Assert.assertEquals(terminal.getWidth() / 2, agc.getBounds().getWidth()/2);
        Assert.assertEquals(terminal.getHeight() / 2, agc.getBounds().getHeight()/2);
    }

}
