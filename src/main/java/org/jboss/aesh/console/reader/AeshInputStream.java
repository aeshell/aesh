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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.console.reader;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.terminal.Key;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshInputStream extends InputStream {

    private final ArrayBlockingQueue<String> blockingQueue;
    private String b;
    private int c;
    private transient boolean stopped = false;

    public AeshInputStream(ArrayBlockingQueue<String> blockingQueue) {
        this.blockingQueue = blockingQueue;
    }

    @Override
    public int read() throws IOException {
        if(stopped && blockingQueue.size() == 0)
            return -1;
        try {
            if (b == null || c == b.length()) {
                b = blockingQueue.take();
                c = 0;
            }

            if (b != null && !(b.length() == 0)) {
                return b.charAt(c++);
            }
        } catch (InterruptedException e) {
            //
        }
        return -1;
    }

    public int[] readAll() {
        if(stopped && blockingQueue.size() == 0)
            return new int[] {-1};
        try {
            if(Config.isOSPOSIXCompatible()) {
                String out = blockingQueue.take();
                int[] input = new int[out.length()];
                for(int i=0; i < out.length(); i++)
                    input[i] = out.charAt(i);
                return input;
            }
            else {
                String out = blockingQueue.take();
                //hack to make multi-value input work (arrows ++)
                if (!out.isEmpty() &&
                        (out.charAt(0) == Key.WINDOWS_ESC.getAsChar() ||
                                out.charAt(0) == Key.WINDOWS_ESC_2.getAsChar())) {
                    int[] input = new int[2];
                    //set the first char to WINDOWS_ESC, then we can reduce the number of different key's in the future
                    input[0] = Key.WINDOWS_ESC.getAsChar();
                    String out2 = blockingQueue.take();
                    input[1] = out2.charAt(0);

                    return input;
                }
                else {
                    int[] input = new int[out.length()];
                    for(int i=0; i < out.length(); i++)
                        input[i] = out.charAt(i);
                    return input;
                }
            }

        }
        catch (InterruptedException e) {
            return new int[] {-1};
        }
    }

    @Override
    public int available() {
        if (b != null)
            return b.length();
        else
            return 0;
    }

    @Override
    public void close() throws IOException {
        if(!stopped) {
            stopped = true;
            blockingQueue.add("");
        }
    }

    //TODO: not sure if this is very smart...
    public void write(String toBuffer) {
        blockingQueue.add(toBuffer);
    }

}
