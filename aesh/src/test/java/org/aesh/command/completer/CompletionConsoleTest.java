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
package org.aesh.command.completer;

import java.io.IOException;

import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.readline.Prompt;
import org.aesh.readline.completion.Completion;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.AeshCommandLineParser;

import org.aesh.command.Command;
import org.aesh.readline.ReadlineConsole;
import org.aesh.readline.terminal.Key;
import org.aesh.tty.TestConnection;
import org.aesh.terminal.utils.Config;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CompletionConsoleTest {

    @Test
    public void completionWithOptions() throws IOException, CommandLineParserException, InterruptedException {

        final ProcessedCommand<Command<CommandInvocation>, CommandInvocation> param = ProcessedCommandBuilder.builder().name("less")
                .description("less --options <files>")
                .create();

        param.addOption(ProcessedOptionBuilder.builder().shortName('f').name("foo").hasValue(true).type(String.class).build());

        final CommandLineParser<CommandInvocation> parser = new AeshCommandLineParser<>(param);
        final StringBuilder builder = new StringBuilder();

        Completion completion = co -> {
            if(parser.getProcessedCommand().name().startsWith(co.getBuffer())) {
                co.addCompletionCandidate(parser.getProcessedCommand().name());
            }
            // commandline longer than the name
            else if(co.getBuffer().startsWith(parser.getProcessedCommand().name())){
               if(co.getBuffer().length() > parser.getProcessedCommand().name().length())  {
                  if(co.getBuffer().endsWith(" --")) {
                     for(ProcessedOption o : parser.getProcessedCommand().getOptions()) {
                         co.addCompletionCandidate("less --"+o.name());
                         builder.append(o.name()).append(" ");
                     }
                      co.setOffset(co.getOffset());
                  }
                  else if(co.getBuffer().endsWith(" -")) {
                      for(ProcessedOption o : parser.getProcessedCommand().getOptions()) {
                          co.addCompletionCandidate("less -"+o.shortName());
                          builder.append("-").append(o.shortName()).append(" ");
                      }
                  }
               }
            }
        };
        TestConnection con = new TestConnection();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .connection(con)
                        .logging(true)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);

        console.addCompletion(completion);
        console.start();

        con.read("le");
        con.read(Key.CTRL_I);
        con.assertBuffer("less ");

        con.read("--");
        con.read(Key.CTRL_I);
        con.assertBuffer("less --foo ");

        console.stop();
    }

    @Test
    public void askDisplayCompletion() throws Exception {
        Completion completion = co -> {
            if(co.getBuffer().equals("file")) {
                for (int i = 0; i < 105; i++) {
                    co.addCompletionCandidate("file" + i);
                }
            }
        };
        TestConnection con = new TestConnection();

        Settings<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                        OptionActivator, CommandActivator> settings =
                SettingsBuilder.builder()
                        .connection(con)
                        .logging(true)
                        .build();

        ReadlineConsole console = new ReadlineConsole(settings);

        console.addCompletion(completion);

        console.setPrompt(new Prompt("# "));
        console.start();

        con.read("file");
        con.read(Key.CTRL_I);
        Thread.sleep(200);

        assertTrue(con.getOutputBuffer().startsWith("# file"));
        assertTrue(con.getOutputBuffer().endsWith("Display all 105 possibilities? (y or n)"));
        con.clearOutputBuffer();

        con.read("n");

        Thread.sleep(200);

        assertEquals(Config.getLineSeparator()+"# file", con.getOutputBuffer());
        console.stop();
    }

}
