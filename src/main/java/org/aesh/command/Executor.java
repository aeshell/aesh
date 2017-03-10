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
import java.util.List;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.validator.CommandValidatorException;

/**
 * Contains the list of Execution to execute.
 *
 * @author jdenise@redhat.com
 */
public class Executor<T extends CommandInvocation> {

    private final List<Execution<T>> executions;

    public Executor(List<Execution<T>> executions) {
        this.executions = Collections.unmodifiableList(executions);
    }

    public void execute() throws CommandException, CommandValidatorException, InterruptedException {

        for (Execution<T> exec : executions) {
            exec.execute();
        }
    }

    public List<Execution<T>> getExecutions() {
        return executions;
    }
}
