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
package org.aesh.builtins.mkdir;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.shell.Shell;
import org.aesh.io.Resource;
import org.aesh.terminal.utils.Config;

import java.util.List;

/**
 * A simple mkdir command.
 *
 * @author <a href="mailto:00hf11@gmail.com">Helio Frota</a>
 */
@CommandDefinition(name = "mkdir", description = "create directory(ies), if they do not already exist.")
public class Mkdir implements Command<CommandInvocation> {

    @Option(shortName = 'h', name = "help", hasValue = false, description = "display this help and exit")
    private boolean help;

    @Option(shortName = 'p', name = "parents", hasValue = false,
            description = "make parent directories as needed")
    private boolean parents;

    @Option(shortName = 'v', name = "verbose", hasValue = false,
            description = "print a message for each created directory")
    private boolean verbose;

    @Arguments(description = "directory(ies) to create")
    private List<Resource> arguments;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) {
        if (help || arguments == null || arguments.isEmpty()) {
            commandInvocation.getShell().writeln(commandInvocation.getHelpInfo("mkdir"));
            return CommandResult.SUCCESS;
        }

        for (Resource f : arguments) {
            Resource currentWorkingDirectory = commandInvocation.getConfiguration().getAeshContext().getCurrentWorkingDirectory();
            Shell shell = commandInvocation.getShell();

            Resource pathResolved = f.resolve(currentWorkingDirectory).get(0);

            if (parents || f.getName().contains(Config.getPathSeparator())) {
                makeDirs(arguments, pathResolved, shell);
            } else {
                makeDir(pathResolved, shell);
            }
        }

        return CommandResult.SUCCESS;
    }

    private void makeDir(Resource dir, Shell shell) {
        if (!dir.exists()) {
            dir.mkdirs();
            if (verbose) {
                shell.writeln("created directory '" + dir.getName() + "'");
            }
        } else {
            shell.writeln("cannot create directory '" + dir.getName() + "': Directory exists");
        }
    }

    private void makeDirs(List<Resource> resources, Resource dir, Shell shell) {
        if (!dir.exists()) {
            dir.mkdirs();
            if (verbose) {
                for (Resource r : resources) {
                    shell.writeln("created directory '" + r.getName() + "'");
                }
            }
        }
    }

}
