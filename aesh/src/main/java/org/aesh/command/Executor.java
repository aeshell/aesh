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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;

/**
 * Contains the list of Execution to execute and the logic to deal with
 * operators.
 *
 * @author jdenise@redhat.com
 */
public class Executor<T extends CommandInvocation> {

    private final List<Execution<T>> executions;

    private final Set<Execution<T>> skip = new HashSet<>();
    public Executor(List<Execution<T>> executions) {
        this.executions = Collections.unmodifiableList(executions);
    }

    public void execute() throws CommandException, CommandValidatorException, InterruptedException, RuntimeException,
                                         CommandLineParserException, OptionValidatorException {
        Execution<T> exec;
        while ((exec = getNextExecution()) != null) {
            exec.execute();
        }
    }

    public List<Execution<T>> getExecutions() {
        return executions;
    }

    public boolean hasNext() {
        return getNextExecution() != null;
    }

    public Execution<T> getNextExecution() {
        if (executions.isEmpty()) {
            return null;
        }

        int index = 0;
        // Retrieve the first non executed non skip execution.
        for (Execution<T> execution : executions) {
            if (execution.getResult() == null && !skip.contains(execution)) {
                break;
            }
            index += 1;
        }

        //that is the first one, just return it
        if (index == 0) {
            return executions.get(index);
        }
        // no more to execute
        if (index == executions.size()) {
            return null;
        }
        // We need the result of the last executed command. It will be conveyed
        // to the next command to execute.
        //The last executed one is at index - n; n being the number of skip.
        int n = 1;
        while (executions.get(index - n).getResult() == null) {
            n += 1;
        }
        int i = index - 1;
        CommandResult lastResult = executions.get(index - n).getResult();
        while (i < executions.size() - 1) {
            Execution exec = executions.get(i);
            if (exec.getExecutable().canExecuteNext(lastResult)) {
                return executions.get(i + 1);
            } else {
                i += 1;
                skip.add(executions.get(i));
            }
        }
        return null;
    }

    public void clearSkippedListData() {
       if(skip.size() > 0)
           for(Execution<T> execution : skip) {
               execution.clearQueuedLine();
           }
    }

    public boolean hasSkipped() {
        return skip.size() > 0;
    }
}
