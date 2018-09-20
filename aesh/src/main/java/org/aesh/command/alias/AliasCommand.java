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
package org.aesh.command.alias;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.readline.alias.AliasManager;

import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@CommandDefinition(name = "alias", aliases = {"unalias"}, description = "")
public class AliasCommand implements Command<CommandInvocation>{

   @Option(shortName = 'p', hasValue = false, description = "display help information")
    private boolean print;

    @Arguments(completer = AliasCompletor.class)
    private List<String> arguments;


    private final AliasManager manager;

    public AliasCommand(AliasManager manager) {
        this.manager = manager;
    }

    public AliasManager manager() {
        return manager;
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if(print || arguments == null || arguments.size() == 0) {
            String out = manager.printAllAliases();
            if(out != null && out.length() > 0)
                commandInvocation.println(out);
        }
        else if(arguments.size() == 1) {
            String out = manager.parseAlias("alias " + arguments.get(0));
            if(out != null && out.length() > 0)
                commandInvocation.println(out);
        }

        return CommandResult.SUCCESS;
    }

    public static class AliasCompletor implements OptionCompleter<CompleterInvocation> {

        @Override
        public void complete(CompleterInvocation completerInvocation) {

            if(completerInvocation.getCommand() instanceof AliasCommand) {
                AliasManager manager = ((AliasCommand) completerInvocation.getCommand()).manager();
                if(completerInvocation.getGivenCompleteValue() == null ||
                           completerInvocation.getGivenCompleteValue().length() == 0)
                    completerInvocation.addAllCompleterValues(manager.findAllMatchingNames(""));
                else
                    completerInvocation.addAllCompleterValues(manager.findAllMatchingNames(completerInvocation.getGivenCompleteValue()));
            }
        }
    }
}
