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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.builtins.pushd;


import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.completer.FileOptionCompleter;
import org.aesh.command.impl.converter.FileResourceConverter;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.io.Resource;

import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@CommandDefinition(name = "pushd", description = "usage: pushd [dir]")
public class Pushd implements Command<CommandInvocation> {

    @Option(shortName = 'h', hasValue = false)
    private boolean help;

    @Arguments(completer = FileOptionCompleter.class, converter = FileResourceConverter.class)
    private List<Resource> arguments;

    private final DirectoryStack stack = DirectoryStack.getInstance();

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws InterruptedException {
        if(help) {
            commandInvocation.getShell().writeln( commandInvocation.getHelpInfo("pushd"));
            return CommandResult.SUCCESS;
        }
        else if(arguments != null && arguments.size() > 0) {

            List<Resource> files = arguments.get(0).resolve(commandInvocation.getConfiguration().getAeshContext().getCurrentWorkingDirectory());

            if(files.get(0).isDirectory()) {
                Resource oldCwd = commandInvocation.getConfiguration().getAeshContext().getCurrentWorkingDirectory();
                stack.push(oldCwd);
                commandInvocation.getConfiguration().getAeshContext().setCurrentWorkingDirectory(files.get(0));
                commandInvocation.getShell().writeln(files.get(0)+" "+stack.asString());
                return CommandResult.SUCCESS;
            }

            return CommandResult.FAILURE;
        }
        else {
            commandInvocation.getShell().writeln("pushd: no other directory");
            return CommandResult.FAILURE;
        }
    }

    public Resource popDirectory() {
        return stack.pop();
    }
}
