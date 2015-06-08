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

import org.jboss.aesh.console.command.CommandOperation;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class AeshConsoleCallback implements ConsoleCallback {

    private Process process;

    public AeshConsoleCallback() {
    }

    @Override
    public CommandOperation getInput() throws InterruptedException {
        if( process != null ) {
            return process.getInput();
        }
        else {
            return new CommandOperation(null, null, 0);
        }
    }

    @Override
    public String getInputLine()  throws InterruptedException {
        if(process != null)
            return process.getInputLine();
        else
            return null;
    }

    @Override
    public void setProcess(Process process) {
        this.process = process;
    }

}
