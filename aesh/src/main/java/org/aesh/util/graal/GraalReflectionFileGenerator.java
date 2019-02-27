/*
 * Copyright 2019 Red Hat, Inc.
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
package org.aesh.util.graal;

import org.aesh.command.Command;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;

import static org.aesh.utils.Config.getLineSeparator;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class GraalReflectionFileGenerator {

    private boolean booleanOption = false;
    private boolean fileOption = false;
    private boolean hasOptions = false;

    public String generateReflection(CommandLineParser<CommandInvocation> parser) {
        StringBuilder builder = new StringBuilder("[").append(getLineSeparator());
        parseCommand(parser.getProcessedCommand(), builder);
        if(parser.isGroupCommand())
            for(CommandLineParser<CommandInvocation> child : parser.getAllChildParsers()) {
                builder.append("  },").append(getLineSeparator());
                parseCommand(child.getProcessedCommand(), builder);
            }
        if(hasOptions)
            appendOptions(parser.getProcessedCommand(), builder);
        else
            builder.append(getLineSeparator()).append("  }");

        return builder.append(getLineSeparator()).append("]").toString();
    }

    private void parseCommand(ProcessedCommand<Command<CommandInvocation>, CommandInvocation> command, StringBuilder builder) {
        builder.append("  {").append(getLineSeparator());
        appendCommand(command, builder);
    }

    private void appendOptions(ProcessedCommand<Command<CommandInvocation>, CommandInvocation> command, StringBuilder builder) {
        builder.append("  },").append(getLineSeparator());
        builder.append("  {").append(getLineSeparator())
                .append("    \"name\" : \"org.aesh.command.impl.parser.AeshOptionParser\", ").append(getLineSeparator());
        appendDefaults(builder);
        builder.append(getLineSeparator()).append("  }");
        if(booleanOption) {
            builder.append(',').append(getLineSeparator());
            builder.append("  {").append(getLineSeparator())
                    .append("    \"name\" : \"org.aesh.command.impl.completer.BooleanOptionCompleter\", ").append(getLineSeparator());
            appendDefaults(builder);
            builder.append(getLineSeparator()).append("  }");
        }
        if(fileOption) {
            builder.append(',').append(getLineSeparator());
            builder.append("  {").append(getLineSeparator())
                    .append("    \"name\" : \"org.aesh.command.impl.completer.FileOptionCompleter\", ").append(getLineSeparator());
            appendDefaults(builder);
            builder.append(getLineSeparator()).append("  }");
        }
    }

    private void appendDefaults(StringBuilder builder) {
        builder.append("    \"allDeclaredConstructors\" : true,").append(getLineSeparator())
                .append("    \"allPublicConstructors\" : true,").append(getLineSeparator())
                .append("    \"allDeclaredMethods\" : true,").append(getLineSeparator())
                .append("    \"allPublicMethods\" : true");
    }

    private void appendCommand(ProcessedCommand<Command<CommandInvocation>, CommandInvocation> command, StringBuilder builder) {
        builder.append("    \"name\" : ").append(command.getCommand().getClass().toString()).append("\",").append(getLineSeparator());
        appendDefaults(builder);

        if(command.getOptions().size() > 0) {
            hasOptions = true;
            builder.append(",").append(getLineSeparator())
                    .append("    \"fields\" : [").append(getLineSeparator());
            for(int i=0; i < command.getOptions().size(); i++) {
                builder.append("      { \"name\" : \"").append(command.getOptions().get(i).getFieldName()).append("\" }");
                if(i < command.getOptions().size())
                    builder.append(",").append(getLineSeparator());
                if(command.getOptions().get(i).type().getName().equalsIgnoreCase("boolean"))
                    booleanOption = true;
                if(command.getOptions().get(i).type().getName().equalsIgnoreCase("java.io.file")||
                    command.getOptions().get(i).type().getName().equalsIgnoreCase("org.aesh.io.resource"))
                    fileOption = true;
            }
            builder.append("    ]").append(getLineSeparator());
        }
    }
}
