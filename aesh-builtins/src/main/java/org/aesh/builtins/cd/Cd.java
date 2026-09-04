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
package org.aesh.builtins.cd;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.io.Resource;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.utils.Config;

import java.util.List;


/**
 * Use AeshConsole.getAeshContext().cwd as reference
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@CommandDefinition(name = "cd", description = "change directory [dir]")
public class Cd implements Command<CommandInvocation> {

    @Option(shortName = 'h', name = "help", hasValue = false, description = "display this help and exit")
    private boolean help;

    @Arguments(description = "directory to change to")
    private List<Resource> arguments;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("cd"));
            return CommandResult.SUCCESS;
        }

        if (arguments == null) {
            updatePrompt(commandInvocation,
                    commandInvocation.getConfiguration().getAeshContext().getCurrentWorkingDirectory().newInstance(Config.getHomeDir()));
        }
        else {
            List<Resource> files =
                    arguments.get(0).resolve(commandInvocation.getConfiguration().getAeshContext().getCurrentWorkingDirectory());

            if(files.get(0).isDirectory())
                updatePrompt(commandInvocation, files.get(0));
        }
        return CommandResult.SUCCESS;
    }

    private void updatePrompt(CommandInvocation commandInvocation, Resource file) {
        commandInvocation.getConfiguration().getAeshContext().setCurrentWorkingDirectory(file);
        commandInvocation.setPrompt(new Prompt("[aesh@extensions:"+file.toString()+"]$ "));
    }
}
