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
package org.jboss.aesh.console.reader;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.terminal.Key;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshInputStream extends InputStream {

    private transient boolean reading;
    private final InputStream consoleStream;
    private static final int BUFFER_SIZE = 1024;
    private final byte[] bBuf = new byte[BUFFER_SIZE];
    private static final int[] NULL_INPUT = new int[] {-1};

    //private static final Logger LOGGER = LoggerUtil.getLogger(AeshInputStream2.class.getName());

    public AeshInputStream(InputStream consoleStream) {
        this.consoleStream = consoleStream;
        reading = true;
    }

    public int[] readAll() {
        if(!reading)
            return new int[] {-1};
        try {
            if(Config.isOSPOSIXCompatible()) {
                //LOGGER.info("trying to read");
                String out = readFromStream();
                //LOGGER.info("read: "+out);
                if(out == null)
                    return NULL_INPUT;
                int[] input = new int[out.length()];
                for(int i=0; i < out.length(); i++)
                    input[i] = out.charAt(i);
                return input;
            }
            else {
                String out = readFromStream();
                if(out == null)
                    return NULL_INPUT;
                //hack to make multi-value input work (arrows ++)
                if (!out.isEmpty() &&
                        (out.charAt(0) == Key.WINDOWS_ESC.getAsChar() ||
                                out.charAt(0) == Key.WINDOWS_ESC_2.getAsChar())) {
                    int[] input = new int[2];
                    if (out.length() == 2) {
                        //If there are two characters, then the first is WINDOWS_ESC, and the second is what follows it.
                        input[0] = Key.WINDOWS_ESC.getAsChar();
                        input[1] = out.charAt(1);
                    }
                    else {
                        //Otherwise, set the first char to WINDOWS_ESC, then we can reduce the number of different key's in the future
                        input[0] = Key.WINDOWS_ESC.getAsChar();
                        String out2 = readFromStream();
                        if(out2 == null)
                            return NULL_INPUT;
                        input[1] = out2.charAt(0);
                    }

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
        catch (IOException e) {
            e.printStackTrace();
            return new int[] {-1};
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return new int[] {-1};
        }
    }

    private String readFromStream() throws IOException, InterruptedException {
        if(Config.isOSPOSIXCompatible()) {
            while (reading) {
                int read = consoleStream.available();
                if (read > 0) {
                    if(read > BUFFER_SIZE)
                        read = BUFFER_SIZE;
                    consoleStream.read(bBuf, 0, read);
                    return new String(bBuf, 0, read);
                }
                else if (read < 0) {
                    return null;
                }
                Thread.sleep(5);
            }
        }
        else {
            while (reading) {
                int read = consoleStream.read(bBuf);
                if (read > 0) {
                    return new String(bBuf, 0, read);
                }
                else if (read < 0) {
                    return null;
                }
                Thread.sleep(10);
            }
        }
        return null;
    }

    public void stop() {
        reading = false;
    }

    @Override
    public int read() throws IOException {
        return consoleStream.read();
    }

    @Override
    public int available() throws IOException {
        return consoleStream.available();
    }
}
