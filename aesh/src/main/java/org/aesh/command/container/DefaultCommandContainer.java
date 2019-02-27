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
package org.aesh.command.container;

import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.readline.AeshContext;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.CommandException;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.CommandResult;
import org.aesh.parser.ParsedLine;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public abstract class DefaultCommandContainer<CI extends CommandInvocation> implements CommandContainer<CI> {

    @Override
    public CommandContainerResult executeCommand(ParsedLine line, InvocationProviders invocationProviders,
                                                 AeshContext aeshContext,
                                                 CI commandInvocation)
            throws CommandLineParserException, OptionValidatorException, CommandValidatorException, CommandException, InterruptedException {

        getParser().parse(line.iterator(), CommandLineParser.Mode.STRICT);
        return executeCommand(invocationProviders, aeshContext, commandInvocation);
    }

    private CommandContainerResult executeCommand(InvocationProviders invocationProviders,
            AeshContext aeshContext,
            CI commandInvocation)
            throws CommandLineParserException, OptionValidatorException, CommandValidatorException, CommandException, InterruptedException {

        getParser().parsedCommand().getCommandPopulator().populateObject(getParser().parsedCommand().getProcessedCommand(), invocationProviders, aeshContext, CommandLineParser.Mode.VALIDATE);
        if (getParser().parsedCommand().getProcessedCommand().validator() != null  && !getParser().parsedCommand().getProcessedCommand().hasOptionWithOverrideRequired()) {
            getParser().parsedCommand().getProcessedCommand().validator().validate(getParser().parsedCommand().getCommand());
        }

        CommandResult result = getParser().parsedCommand().getCommand().execute(commandInvocation);

        return new CommandContainerResult(getParser().parsedCommand().getProcessedCommand().resultHandler(), result);
    }

    @Override
    public String printHelp(String childCommandName) {
       if(getParser().isGroupCommand() && childCommandName.contains(" ")) {
           String[] names = childCommandName.split(" ");
           if(names.length > 1 && names[1].length() > 0) {
              CommandLineParser child = getParser().getChildParser(names[1]);
               if(child != null)
                   return child.printHelp();
           }
           return "Child command "+names[1]+" not found.";
       }
       else
           return getParser().printHelp();
    }
}
