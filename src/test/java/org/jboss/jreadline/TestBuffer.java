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

import java.io.ByteArrayOutputStream;

/**
 * Buffer to simplify testing
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TestBuffer {
    public static final short ESCAPE = 27;
    public static final short ENTER = 10;
    public static final short EMACS_UP = 16;

    private ByteArrayOutputStream outputStream;

    public TestBuffer() {
        outputStream = new ByteArrayOutputStream();
    }

    public TestBuffer(String in) {
        outputStream = new ByteArrayOutputStream();
        append(in);
    }

    public byte[] getBytes() {
        return outputStream.toByteArray();
    }

    public TestBuffer append(String str) {
        for(byte b : str.getBytes())
            outputStream.write(b);

        return this;
    }

    public TestBuffer append(int i) {
        outputStream.write(i);
        return this;
    }

}
