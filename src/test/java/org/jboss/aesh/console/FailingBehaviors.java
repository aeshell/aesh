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

import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class FailingBehaviors extends BaseConsoleTest {

    /**
     * Wrong key parsing
     */
    @Test
    public void testKeyParsing() throws Exception {
        Assume.assumeTrue(Config.isOSPOSIXCompatible());

        invokeTestConsole(new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws IOException {
                out.write(new byte[] { '3', '4', 1, 27 });
                out.flush();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                out.write(new byte[]{91, 67, '1', '2', '\r'});
                out.flush();
            }
        }, new Verify() {
            @Override
            public int call(Console console, ConsoleOperation op) {
                assertEquals("3124", op.getBuffer());
                return 0;
            }
        });
    }

    /**
     * Unclose quote
     *
     * We need a way to disable automatic multi-line if the user wants to control it,
     * or to have a pluggable parser.
     *
     */
    @Test
    public void testUnclosedQuote() throws Exception {
        Assume.assumeTrue(Config.isOSPOSIXCompatible());

        invokeTestConsole(new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws IOException {
                out.write(new byte[] { '3', '4', '"', '\r' });
                out.flush();
            }
        }, new Verify() {
            @Override
            public int call(Console console, ConsoleOperation op) {
                assertEquals("34\"", op.getBuffer());
                return 0;
            }
        });
    }

    /**
     * Console.getCursor() is doomed to problems
     */
    @Test
    public void testGetCursor() throws Exception {
        Assume.assumeTrue(Config.isOSPOSIXCompatible());

        invokeTestConsole(new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws IOException {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < 20; i++) {
                    for (int j = 0; j < 10; j++ ) {
                        Thread.yield();
                        out.write(new byte[] { 27, 91, 68 }); // left
                        out.write(new byte[] { (byte)('0' + j) });
                        out.flush();
                    }
                }
                out.write(new byte[]{'\n'});
                out.flush();
            }
        }, new Verify() {
            @Override
            public int call(Console console, ConsoleOperation op) {
                assertNotNull(op.getBuffer());
                return 0;
            }
        });
    }

    /**
     * Bad thread safety
     */
    @Test
    public void testThreadSafety() throws Exception {
        Assume.assumeTrue(Config.isOSPOSIXCompatible());

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
                                    assertEquals(finalI, line.charAt(k));
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
    }

}
