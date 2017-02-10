/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
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
package org.aesh.command;

import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.result.ResultHandler;

/**
 * Contains both the executable and the execution context.
 *
 * @author jdenise@redhat.com
 */
public class Executor<T extends CommandInvocation> {

    private final T commandInvocation;
    private final Executable<T> exe;
    private final ResultHandler resultHandler;

    public Executor(T commandInvocation, Executable<T> exe, ResultHandler resultHandler) {
        this.commandInvocation = commandInvocation;
        this.exe = exe;
        this.resultHandler = resultHandler;
    }

    public Executable<T> getExecutable() {
        return exe;
    }

    public T getCommandInvocation() {
        return commandInvocation;
    }

    public CommandResult execute() throws CommandException, InterruptedException {

        CommandResult result = exe.execute(commandInvocation);

        if(resultHandler != null) {
            if( result.equals(CommandResult.SUCCESS))
                resultHandler.onSuccess();
            else
                resultHandler.onFailure(result);
        }
        return result;
    }
}
