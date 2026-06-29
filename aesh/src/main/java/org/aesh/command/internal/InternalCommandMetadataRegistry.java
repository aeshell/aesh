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
package org.aesh.command.internal;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Consumer;

import org.aesh.command.Command;
import org.aesh.command.alias.AliasCommand;
import org.aesh.command.alias.UnAliasCommand;
import org.aesh.command.export.ExportCommand;
import org.aesh.command.impl.internal.FieldAccessor;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.metadata.CommandMetadataProvider;
import org.aesh.command.metadata.MetadataRegistry;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.converter.CLConverterManager;

/**
 * Hand-written metadata registry for aesh's internal commands.
 * <p>
 * These commands cannot use the annotation processor because the aesh module
 * cannot depend on aesh-processor (circular dependency). The metadata is
 * equivalent to what the processor would generate.
 *
 * @author Aesh team
 * @since 3.16
 */
public final class InternalCommandMetadataRegistry implements MetadataRegistry {

    @Override
    public CommandMetadataProvider<?> get(String commandClassName) {
        switch (commandClassName) {
            case "org.aesh.command.alias.AliasCommand":
                return new AliasCommandMetadata();
            case "org.aesh.command.alias.UnAliasCommand":
                return new UnAliasCommandMetadata();
            case "org.aesh.command.export.ExportCommand":
                return new ExportCommandMetadata();
            default:
                return null;
        }
    }

    /** Create a FieldAccessor backed by a reflected Field. */
    private static FieldAccessor fieldAccessor(Field f) {
        return new FieldAccessor() {
            @Override
            public void set(Object instance, Object value) {
                try {
                    f.set(instance, value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Object get(Object instance) {
                try {
                    return f.get(instance);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /** Create a resetter that sets a field to null. */
    private static Consumer<Object> nullResetter(Field f) {
        return instance -> {
            try {
                f.set(instance, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    /** Create a resetter that sets a boolean field to false. */
    private static Consumer<Object> falseResetter(Field f) {
        return instance -> {
            try {
                f.set(instance, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static Field field(Class<?> cls, String name) {
        try {
            Field f = cls.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    // ---- AliasCommand ----

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final class AliasCommandMetadata implements CommandMetadataProvider<AliasCommand> {
        private static final Field F_PRINT = field(AliasCommand.class, "print");
        private static final Field F_ARGUMENTS = field(AliasCommand.class, "arguments");

        public Class<AliasCommand> commandType() {
            return AliasCommand.class;
        }

        public AliasCommand newInstance() {
            throw new UnsupportedOperationException("AliasCommand requires AliasManager");
        }

        public boolean isGroupCommand() {
            return false;
        }

        public Class<? extends Command>[] groupCommandClasses() {
            return new Class[0];
        }

        public String commandName() {
            return "alias";
        }

        public String[] commandAliases() {
            return new String[] { "unalias" };
        }

        public ProcessedCommand buildProcessedCommand(AliasCommand instance)
                throws CommandLineParserException {
            ProcessedOption printOpt = ProcessedOption.createDirect(
                    "p", "print", "display help information",
                    Boolean.class, "print", OptionType.BOOLEAN,
                    CLConverterManager.getInstance().getConverter(Boolean.class),
                    fieldAccessor(F_PRINT));
            printOpt.setFieldResetter(falseResetter(F_PRINT));

            ProcessedOption argsOpt = ProcessedOption.createDirect(
                    null, "arguments", "",
                    String.class, "arguments", OptionType.ARGUMENTS,
                    CLConverterManager.getInstance().getConverter(String.class),
                    fieldAccessor(F_ARGUMENTS));
            argsOpt.setFieldResetter(nullResetter(F_ARGUMENTS));
            argsOpt.setCompleter(new AliasCommand.AliasCompletor());

            ProcessedCommand pc = ((ProcessedCommandBuilder) ProcessedCommandBuilder.builder())
                    .name("alias")
                    .aliases(Arrays.asList("unalias"))
                    .description("")
                    .command(instance)
                    .generateHelp(false)
                    .disableParsing(false)
                    .create();

            pc.addOption(printOpt);
            pc.setArguments(argsOpt);
            return pc;
        }
    }

    // ---- UnAliasCommand ----

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final class UnAliasCommandMetadata implements CommandMetadataProvider<UnAliasCommand> {
        private static final Field F_ARGUMENTS = field(UnAliasCommand.class, "arguments");

        public Class<UnAliasCommand> commandType() {
            return UnAliasCommand.class;
        }

        public UnAliasCommand newInstance() {
            throw new UnsupportedOperationException("UnAliasCommand requires AliasManager");
        }

        public boolean isGroupCommand() {
            return false;
        }

        public Class<? extends Command>[] groupCommandClasses() {
            return new Class[0];
        }

        public String commandName() {
            return "unalias";
        }

        public ProcessedCommand buildProcessedCommand(UnAliasCommand instance)
                throws CommandLineParserException {
            ProcessedOption argsOpt = ProcessedOption.createDirect(
                    null, "arguments", "",
                    String.class, "arguments", OptionType.ARGUMENTS,
                    CLConverterManager.getInstance().getConverter(String.class),
                    fieldAccessor(F_ARGUMENTS));
            argsOpt.setFieldResetter(nullResetter(F_ARGUMENTS));
            argsOpt.setCompleter(new UnAliasCommand.AliasCompletor());

            ProcessedCommand pc = ((ProcessedCommandBuilder) ProcessedCommandBuilder.builder())
                    .name("unalias")
                    .description("remove an alias")
                    .command(instance)
                    .generateHelp(false)
                    .disableParsing(false)
                    .create();

            pc.setArguments(argsOpt);
            return pc;
        }
    }

    // ---- ExportCommand ----

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final class ExportCommandMetadata implements CommandMetadataProvider<ExportCommand> {
        private static final Field F_ARGUMENTS = field(ExportCommand.class, "arguments");

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

        public ProcessedCommand buildProcessedCommand(ExportCommand instance)
                throws CommandLineParserException {
            ProcessedOption argsOpt = ProcessedOption.createDirect(
                    null, "arguments", "",
                    String.class, "arguments", OptionType.ARGUMENTS,
                    CLConverterManager.getInstance().getConverter(String.class),
                    fieldAccessor(F_ARGUMENTS));
            argsOpt.setFieldResetter(nullResetter(F_ARGUMENTS));

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
