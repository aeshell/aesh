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
package org.aesh.command.builder;

import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.parser.CommandLineParserBuilder;
import org.aesh.utils.ANSI;
import org.aesh.utils.Config;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineFormatterTest {


    @Test
    public void formatter() throws CommandLineParserException {
        ProcessedCommandBuilder pb = new ProcessedCommandBuilder().name("man").description("[OPTION...]");

        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('d')
                        .name("debug")
                        .description("emit debugging messages")
                        .type(String.class)
                        .build());

        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('D')
                        .name("default")
                        .description("reset all options to their default values")
                        .type(String.class)
                        .build());

        CommandLineParser clp = new CommandLineParserBuilder()
                .processedCommand(pb.create())
                .create();

        assertEquals("Usage: man" + Config.getLineSeparator() + "[OPTION...]"+ Config.getLineSeparator()+
                        Config.getLineSeparator()+
                        "Options:"+ Config.getLineSeparator()+
                        "  -d, --debug    emit debugging messages"+Config.getLineSeparator()+
                        "  -D, --default  reset all options to their default values"+Config.getLineSeparator(),
                clp.printHelp());
    }

    @Test
    public void formatter2() throws CommandLineParserException {
        ProcessedCommandBuilder pb = new ProcessedCommandBuilder().name("man").description("[OPTION...]");

        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('d')
                        .name("debug")
                        .description("emit debugging messages")
                        .type(String.class)
                        .build());

        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('D')
                        .name("default")
                        .required(true)
                        .description("reset all options to their default values")
                        .type(String.class)
                        .build()
        );

        pb.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('f')
                        .name("file")
                        .hasValue(true)
                        .argument("filename")
                        .description("set the filename")
                        .type(String.class)
                        .build());


        CommandLineParser clp = new CommandLineParserBuilder().processedCommand(pb.create()).create();

        assertEquals("Usage: man" + Config.getLineSeparator() + "[OPTION...]"+ Config.getLineSeparator()+
                        Config.getLineSeparator()+
                        "Options:"+ Config.getLineSeparator()+
                        "  -d, --debug            emit debugging messages"+Config.getLineSeparator()+
                        ANSI.BOLD+
                        "  -D, --default"+
                        ANSI.BOLD_OFF+
                        "          reset all options to their default values"+Config.getLineSeparator()+
                        "  -f, --file=<filename>  set the filename"+Config.getLineSeparator(),
                clp.printHelp());
    }

    @Test
    public void groupFormatter() throws CommandLineParserException {
        ProcessedCommandBuilder git = new ProcessedCommandBuilder().name("git").description("[OPTION...]");
        git.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('h')
                        .name("help")
                        .description("display help info")
                        .type(boolean.class)
                        .build()
        );

        ProcessedCommandBuilder rebase = new ProcessedCommandBuilder().name("rebase").description("[OPTION...]");
        rebase.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('f')
                        .name("foo")
                        .required(true)
                        .description("reset all options to their default values")
                        .type(String.class)
                        .build()
        );

        ProcessedCommandBuilder branch = new ProcessedCommandBuilder().name("branch").description("branching");
        branch.addOption(
                ProcessedOptionBuilder.builder()
                        .shortName('b')
                        .name("bar")
                        .required(true)
                        .description("reset all options to their default values")
                        .type(String.class)
                        .build()
        );


        CommandLineParser clpGit = new CommandLineParserBuilder().processedCommand(git.create()).create();
        CommandLineParser clpBranch = new CommandLineParserBuilder().processedCommand(branch.create()).create();
        CommandLineParser clpRebase = new CommandLineParserBuilder().processedCommand(rebase.create()).create();

        clpGit.addChildParser(clpBranch);
        clpGit.addChildParser(clpRebase);

         assertEquals("Usage: git" + Config.getLineSeparator() + "[OPTION...]" + Config.getLineSeparator() +
                         Config.getLineSeparator() +
                         "Options:" + Config.getLineSeparator() +
                         "  -h, --help  display help info" + Config.getLineSeparator()
                         + Config.getLineSeparator()+"git commands:"+Config.getLineSeparator()+
                         "    branch  branching"+Config.getLineSeparator()+
                         "    rebase  [OPTION...]"+Config.getLineSeparator(),
                 clpGit.printHelp());


    }

}
