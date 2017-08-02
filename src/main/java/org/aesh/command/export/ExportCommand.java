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
package org.aesh.command.export;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.invocation.AeshCommandInvocation;
import org.aesh.command.option.Arguments;

import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@CommandDefinition(name = "export", description = "" )
public class ExportCommand implements Command<AeshCommandInvocation> {

    private ExportManager manager;

    @Arguments
    private List<String> arguments;

    public ExportCommand(ExportManager manager) {
        this.manager = manager;
    }

    @Override
    public CommandResult execute(AeshCommandInvocation commandInvocation) throws CommandException, InterruptedException {

        if(arguments != null && arguments.size() > 0)
            manager.addVariable("export "+arguments.get(0));
        else {
            //for now just list the exported values
            for (String value : manager.getAllNamesWithEquals())
                commandInvocation.println(value);
        }

        return CommandResult.SUCCESS;
    }

}
