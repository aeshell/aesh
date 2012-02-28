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
package org.jboss.jreadline.console.reader;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 * @author Mike Brock
 */
public class ConsoleInputSession {
    private InputStream consoleStream;
    private InputStream externalInputStream;

    private ArrayBlockingQueue<String> blockingQueue = new ArrayBlockingQueue<String>(1000);

    private volatile boolean connected;
    
    public ConsoleInputSession(InputStream consoleStream) {
        this.consoleStream = consoleStream;
        this.connected = true;

        this.externalInputStream = new InputStream() {
            private String b;
            private int c;

            @Override
            public int read() throws IOException {
                try {
                    if (b == null || c == b.length()) {
                        b = blockingQueue.poll(365, TimeUnit.DAYS);
                        c = 0;
                    }

                    if (b != null && !b.isEmpty()) {
                        return b.charAt(c++);
                    }
                }
                catch (InterruptedException e) {
                    //
                }
                return -1;
            }
            
            @Override
            public int available() { 
                if(b != null)
                    return b.length();
                else
                    return 0;
            }

            @Override
            public void close() throws IOException {
                stop();
            }
        };

        startReader();
    }

    private void startReader() {
        Thread readerThread = new Thread() {
            @Override
            public void run() {
                while (connected) {
                    try {
                        byte[] bBuf = new byte[20];
                        int read = consoleStream.read(bBuf);

                        if (read > 0) {
                            blockingQueue.put(new String(bBuf, 0, read));
                        }

                        Thread.sleep(10);
                    }
                    catch (IOException e) {
                        if (connected) {
                            connected = false;
                            throw new RuntimeException("broken pipe");
                        }
                    }
                    catch (InterruptedException e) {
                        //
                    }
                }
            }
        };

        readerThread.start();
    }

    public void interruptPipe() {
        blockingQueue.offer("\n");
    }

    public void stop() {
        connected = false;
        blockingQueue.offer("");
    }

    public InputStream getExternalInputStream() {
        return externalInputStream;
    }
}
