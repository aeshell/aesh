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
package org.aesh.builtins.echo;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.shell.Shell;

import java.util.List;


/**
 * A simple echo command.
 *
 * @author <a href="mailto:00hf11@gmail.com">Helio Frota</a>
 */
@CommandDefinition(name = "echo", description = "Echo the STRING(s) to standard output.")
public class Echo implements Command<CommandInvocation> {

    @Option(shortName = 'h', name = "help", hasValue = false, description = "display this help and exit")
    private boolean help;

    @Arguments
    private List<String> arguments;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) {

        Shell shell = commandInvocation.getShell();

        if (help || arguments == null || arguments.isEmpty()) {
            shell.writeln(commandInvocation.getHelpInfo("echo"));
            return CommandResult.SUCCESS;
        }

        String stdout = "";
        for (String s : arguments) {
            stdout += s + " ";
        }
        stdout = stdout.trim();

        commandInvocation.println(stdout);

        return CommandResult.SUCCESS;
    }

}
