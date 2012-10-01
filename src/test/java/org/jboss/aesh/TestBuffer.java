/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh;

import org.jboss.aesh.console.Config;

import java.io.ByteArrayOutputStream;

/**
 * Buffer to simplify testing
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TestBuffer {
    public static final short ESCAPE = 27;
    public static final short EMACS_HISTORY_PREV = 16;

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

    public TestBuffer append(int[] nums) {
        for(int i : nums)
            outputStream.write(i);
        return this;
    }

    public static int getNewLine() {
        if(Config.isOSPOSIXCompatible())
            return 10;
        else
            return 13;
    }

}
