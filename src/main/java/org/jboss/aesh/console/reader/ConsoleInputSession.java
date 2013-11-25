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

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 * @author Mike Brock
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class ConsoleInputSession {
    private InputStream consoleStream;
    private AeshInputStream aeshInputStream;
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
        aeshInputStream = new AeshInputStream(blockingQueue);
        startReader();
    }

    private void startReader() {
        Runnable reader = new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] bBuf = new byte[1024];
                    while (!executorService.isShutdown()) {
                        int read = consoleStream.read(bBuf);
                        if (read > 0) {
                            blockingQueue.put(new String(bBuf, 0, read));
                        }
                        else if (read < 0) {
                            stop();
                        }
                    }
                }
                catch (RuntimeException e) {
                    if (!executorService.isShutdown()) {
                        executorService.shutdown();
                        throw e;
                    }
                }
                catch (Exception e) {
                    if (!executorService.isShutdown()) {
                        executorService.shutdown();
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        executorService.execute(reader);
    }


    public void stop() throws IOException, InterruptedException {
        executorService.shutdown();
        consoleStream.close();
    }

    public AeshInputStream getExternalInputStream() {
        return aeshInputStream;
    }
}
