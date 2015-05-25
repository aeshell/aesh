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

import org.jboss.aesh.util.LoggerUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 * @author Mike Brock
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class ConsoleInputSession {
    private final AeshInputStream aeshInputStream;
    private final ExecutorService executorService;

    private final BlockingQueue<int[]> blockingQueue = new LinkedBlockingQueue<>(1000);

    private static final Logger LOGGER = LoggerUtil.getLogger(ConsoleInputSession.class.getName());
    private static final int[] NULL_INPUT = new int[] {-1};

    public ConsoleInputSession(InputStream consoleStream) {
        executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread inputThread = Executors.defaultThreadFactory().newThread(runnable);
                inputThread.setName("Aesh InputStream Reader");
                inputThread.setDaemon(true);
                return inputThread;
            }
        });
        aeshInputStream = new AeshInputStream(consoleStream);
        startReader();
    }

    private void startReader() {
        Runnable reader = new Runnable() {
            @Override
            public void run() {
                try {
                    while (aeshInputStream.isReading()) {
                        blockingQueue.put(aeshInputStream.readAll());
                    }
                }
                catch (RuntimeException e) {
                    LOGGER.log(Level.WARNING, "Got runtime exception in reader: ",e);
                    stop();
                    throw e;
                }
                catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Got exception in reader: ",e);
                    stop();
                }
            }
        };
        executorService.execute(reader);
    }

    public int[] readAll() {
        try {
            return blockingQueue.take();
        }
        catch(InterruptedException e) {
            return NULL_INPUT;
        }
    }

    public void stop() {
        if(!executorService.isShutdown()) {
            try {
                aeshInputStream.stop();
                aeshInputStream.close();
                executorService.shutdownNow();
                try {
                    // Unlock thread on blockingQueue.take() by sending the end input.
                    blockingQueue.put(NULL_INPUT);
                }
                catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Failed when pushing -1 to blockingQueue.", e);
                }
                LOGGER.info("input stream is closed, readers finished...");
            }
            catch(IOException e) {
                LOGGER.log(Level.SEVERE, "Failed when trying to close streams", e);
            }
        }
    }

    public void writeToInput(String data) {
        int[] input = new int[data.length()];
        for(int i=0; i < data.length(); i++)
            input[i] = data.charAt(i);
        try {
            blockingQueue.put(input);
        }
        catch(InterruptedException e) {
            LOGGER.warning("Failed to add to input queue");
        }
    }

}
