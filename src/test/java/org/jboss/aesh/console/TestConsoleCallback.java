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

import java.util.List;
import java.util.concurrent.CountDownLatch;

public abstract class TestConsoleCallback extends AeshConsoleCallback {
    private CountDownLatch latch;
    private List<Throwable> exceptions;

    public TestConsoleCallback(CountDownLatch latch, List<Throwable> exceptions) {
       this.latch = latch;
       this.exceptions = exceptions;
    }

    @Override
    public final int execute(ConsoleOperation output) throws InterruptedException {
       try {
          return verify(output);
       } catch (Throwable e) {
          exceptions.add(e);
          throw e;
       } finally {
          latch.countDown();
       }
    }

    public abstract int verify(ConsoleOperation output);
}
