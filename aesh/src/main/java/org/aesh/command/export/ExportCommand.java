/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.aesh.command.export;

import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.internal.FieldAccessor;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.invocation.AeshCommandInvocation;
import org.aesh.command.metadata.CommandMetadataProvider;
import org.aesh.command.option.Arguments;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.converter.CLConverterManager;

/**
 * @author Aesh team
 */
@CommandDefinition(name = "export", description = "")
public class ExportCommand implements Command<AeshCommandInvocation> {

    private ExportManager manager;

    @Arguments
    private List<String> arguments;

    public ExportCommand(ExportManager manager) {
        this.manager = manager;
    }

    @Override
    public CommandResult execute(AeshCommandInvocation commandInvocation) throws CommandException, InterruptedException {

        if (arguments != null && arguments.size() > 0)
            manager.addVariable("export " + arguments.get(0));
        else {
            //for now just list the exported values
            for (String value : manager.getAllNamesWithEquals())
                commandInvocation.println(value);
        }

        return CommandResult.SUCCESS;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final class Metadata implements CommandMetadataProvider<ExportCommand> {
        private static final FieldAccessor ARGS_ACCESSOR = new FieldAccessor() {
            public void set(Object inst, Object val) {
                ((ExportCommand) inst).arguments = (List<String>) val;
            }

            public Object get(Object inst) {
                return ((ExportCommand) inst).arguments;
            }
        };

        public Class<ExportCommand> commandType() {
            return ExportCommand.class;
        }

        public ExportCommand newInstance() {
            throw new UnsupportedOperationException("ExportCommand requires ExportManager");
        }

        public boolean isGroupCommand() {
            return false;
        }

        public Class<? extends Command>[] groupCommandClasses() {
            return new Class[0];
        }

        public String commandName() {
            return "export";
        }

        public ProcessedCommand buildProcessedCommand(ExportCommand instance) throws CommandLineParserException {
            ProcessedOption argsOpt = ProcessedOption.createDirect(
                    null, "arguments", "",
                    String.class, "arguments", OptionType.ARGUMENTS,
                    CLConverterManager.getInstance().getConverter(String.class),
                    ARGS_ACCESSOR);
            argsOpt.setFieldResetter(inst -> ((ExportCommand) inst).arguments = null);

            ProcessedCommand pc = ((ProcessedCommandBuilder) ProcessedCommandBuilder.builder())
                    .name("export")
                    .description("")
                    .command(instance)
                    .generateHelp(false)
                    .disableParsing(false)
                    .create();

            pc.setArguments(argsOpt);
            return pc;
        }
    }
}
