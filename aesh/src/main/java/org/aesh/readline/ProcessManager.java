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
package org.aesh.readline;

import org.aesh.command.Executor;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.terminal.Connection;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ProcessManager {

    private Connection conn;
    private final Console console;
    private Executor<? extends CommandInvocation> executor;
    public ProcessManager(Console console) {
        this.console = console;
    }

    public void execute(Executor<? extends CommandInvocation> executor, Connection conn) {
        this.conn = conn;
        this.executor = executor;
        executeNext();
    }

    public boolean hasNext() {
        return executor.hasNext();
    }

    public void processFinished(Process process) {
        if(hasNext()) {
            executeNext();
        }
        else {
            //if there are commands that wasn't executed we need to clear their data
            if(executor.hasSkipped())
                executor.clearSkippedListData();
            if(console.running())
                console.read();
            else
                conn.close();
        }
    }

    public void executeNext() {
        if(hasNext()) {
            new Process(this, conn, executor.getNextExecution()).start();
        }
    }
}
