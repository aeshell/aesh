/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.reader;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 * @author Mike Brock
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class ConsoleInputSession {
    private InputStream consoleStream;
    private InputStream externalInputStream;
    private ExecutorService executorService;

    private ArrayBlockingQueue<String> blockingQueue = new ArrayBlockingQueue<String>(1000);

    public ConsoleInputSession(InputStream consoleStream) {
        this.consoleStream = consoleStream;
        executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setDaemon(true);
                return thread;
            }
        });

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
                } catch (InterruptedException e) {
                    //
                }
                return -1;
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
                try {
                    stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        startReader();
    }

    private void startReader() {
        Runnable reader = new Runnable() {
            @Override
            public void run() {
                try {
                    while (!executorService.isShutdown()) {
                        byte[] bBuf = new byte[20];
                        int read = consoleStream.read(bBuf);
                        if (read > 0) {
                            blockingQueue.put(new String(bBuf, 0, read));
                            Thread.sleep(50);
                        } else if (read < 0) {
                            stop();
                        }
                    }
                } catch (RuntimeException e) {
                    if (!executorService.isShutdown()) {
                        executorService.shutdown();
                        throw e;
                    }
                } catch (Exception e) {
                    if (!executorService.isShutdown()) {
                        executorService.shutdown();
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        executorService.execute(reader);
    }

    public void interruptPipe() {
        blockingQueue.offer("\n");
    }

    public void stop() throws IOException, InterruptedException {
        executorService.shutdown();
        blockingQueue.offer("");
        consoleStream.close();
    }

    public InputStream getExternalInputStream() {
        return externalInputStream;
    }
}
