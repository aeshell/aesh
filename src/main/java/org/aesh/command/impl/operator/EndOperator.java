/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017 Red Hat Inc. and/or its affiliates and other contributors
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
package org.aesh.command.impl.operator;

import java.util.concurrent.ExecutionException;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
public class EndOperator<T extends CommandInvocation> implements ExecutableOperator<T> {

    private Command<T> executable;

    private DataProvider dataProvider;

    public EndOperator() {

    }

    @Override
    public CommandResult execute(T ic) throws CommandException, InterruptedException {
        // XXX JFDENISE, do something with Data provider if not null.
        // Such as introspect command to findout the target.
        if (dataProvider != null) {
            try {
                String data = dataProvider.getData().get();
                // XXX TODO
            } catch (ExecutionException ex) {
                throw new CommandException(ex);
            }
        }
        return executable.execute(ic);
    }

    @Override
    public void setCommand(Command<T> executable) {
        this.executable = executable;
    }

    @Override
    public void setDataProvider(DataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

}
