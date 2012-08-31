/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
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
