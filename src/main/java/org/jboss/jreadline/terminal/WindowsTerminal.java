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
package org.jboss.jreadline.terminal;

import java.io.IOException;
import java.io.InputStream;

import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.internal.WindowsSupport;
import org.jboss.jreadline.console.reader.CharInputStreamReader;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class WindowsTerminal implements Terminal {

    private CharInputStreamReader reader;

    public void init(InputStream inputStream) {
        if(inputStream == System.in) {
            System.out.println("Using System.in");
        }

        //setting up reader
        reader = new CharInputStreamReader(inputStream);
    }

    public int read() throws IOException {
        //return reader.read();
        return WindowsSupport.readByte();
    }

    public int getHeight() {
        return WindowsSupport.getWindowsTerminalHeight();
    }

    public int getWidth() {
        return WindowsSupport.getWindowsTerminalWidth();
    }

    public boolean isEchoEnabled() {
        return false;  
    }

    public void reset() throws Exception {
        
    }
}

