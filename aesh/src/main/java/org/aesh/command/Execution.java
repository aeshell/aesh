/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;

/**
 * Contains an execution content.
 *
 * @author Aesh team
 */
public interface Execution<T extends CommandInvocation> {

    T getCommandInvocation();

    Executable getExecutable();

    Command<T> getCommand();

    void populateCommand() throws CommandLineParserException, OptionValidatorException;

    ResultHandler getResultHandler();

    CommandResult execute() throws CommandException, CommandValidatorException,
            InterruptedException, CommandLineParserException, OptionValidatorException;

    CommandResult getResult();

    /**
     * @deprecated Use {@link #setResult(CommandResult)} instead. This method contains a typo.
     */
    @Deprecated
    void setResut(CommandResult result);

    /**
     * Sets the result of this execution.
     *
     * @param result the command result
     */
    default void setResult(CommandResult result) {
        setResut(result);
    }

    void clearQueuedLine();
}
