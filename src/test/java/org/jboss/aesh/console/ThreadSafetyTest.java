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
package org.jboss.aesh.console;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class ThreadSafetyTest extends BaseConsoleTest {

    /**
     * Bad thread safety
     */
    @Test
    public void testThreadSafety() throws Exception {
        Assume.assumeTrue(Config.isOSPOSIXCompatible());

        final AtomicBoolean result = new AtomicBoolean(true);

        List<Thread> threads = new ArrayList<>();
        for (int i = 'a'; i <= 'f'; i++) {
            final char finalI = (char) i;
            threads.add(new Thread() {
                @Override
                public void run() {
                    try {
                        invokeTestConsole(new Setup() {
                            @Override
                            public void call(Console console, OutputStream out) throws IOException {
                                for (int i = 0; i < 20; i++) {
                                    for (int j = 0; j < 10; j++) {
                                        Thread.yield();
                                        out.write(new byte[]{(byte) (finalI)});
                                        out.flush();
                                    }
                                }
                                out.write(new byte[]{'\n'});
                                out.flush();
                            }
                        }, new Verify() {
                            @Override
                            public int call(Console console, ConsoleOperation op) {
                                String line = op.getBuffer();
                                for (int k = 0; k < line.length(); k++) {
                                    if (finalI != line.charAt(k)) {
                                        result.set(false);
                                    }
                                }
                                return 0;
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        for (Thread th : threads) {
            th.start();
        }
        for (Thread th : threads) {
            th.join();
        }
        assertTrue(result.get());

    }

}