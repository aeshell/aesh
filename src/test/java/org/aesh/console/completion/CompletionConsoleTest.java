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
package org.aesh.console.completion;


import java.io.IOException;

import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.console.settings.Settings;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.readline.completion.Completion;
import org.aesh.command.impl.parser.CommandLineParserException;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.AeshCommandLineParser;

import org.aesh.command.Command;
import org.aesh.terminal.Key;
import org.aesh.readline.ReadlineConsole;
import org.aesh.tty.TestConnection;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Ignore
public class CompletionConsoleTest {

    @Test
    public void completionWithOptions() throws IOException, InterruptedException, CommandLineParserException {

        final ProcessedCommand param = new ProcessedCommandBuilder().name("less")
                .description("less --options <files>")
                .create();

        param.addOption(ProcessedOptionBuilder.builder().shortName('f').name("foo").hasValue(true).type(String.class).build());

        final CommandLineParser<Command> parser = new AeshCommandLineParser(param);
        final StringBuilder builder = new StringBuilder();

        Completion completion = co -> {
            if(parser.getProcessedCommand().getName().startsWith(co.getBuffer())) {
                co.addCompletionCandidate(parser.getProcessedCommand().getName());
            }
            // commandline longer than the name
            else if(co.getBuffer().startsWith(parser.getProcessedCommand().getName())){
               if(co.getBuffer().length() > parser.getProcessedCommand().getName().length())  {
                  if(co.getBuffer().endsWith(" --")) {
                     for(ProcessedOption o : parser.getProcessedCommand().getOptions()) {
                         co.addCompletionCandidate("less --"+o.getName());
                         builder.append(o.getName()+" ");
                     }
                      co.setOffset(co.getOffset());
                  }
                  else if(co.getBuffer().endsWith(" -")) {
                      for(ProcessedOption o : parser.getProcessedCommand().getOptions()) {
                          co.addCompletionCandidate("less -"+o.getShortName());
                          builder.append("-"+o.getShortName()+" ");
                      }
                  }
               }
            }
        };
        TestConnection con = new TestConnection();

        Settings settings = SettingsBuilder.builder()
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

    /*
    @Test
    public void askDisplayCompletion() throws Exception {
        Completion completion = new Completion() {
            @Override
            public void complete(CompleteOperation co) {
                if(co.getBuffer().equals("file")) {
                    for (int i = 0; i < 105; i++) {
                        co.addCompletionCandidate("file" + i);
                    }
                }
            }
        };

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = SettingsBuilder.builder()
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .logging(true)
                .build();

        ReadlineConsole console = new ReadlineConsole(settings);

        console.addCompletion(completion);

        console.setConsoleCallback(new CompletionConsoleCallback2(console));
        console.setPrompt(new Prompt("# "));
        console.start();

        try {
            outputStream.write("file".getBytes());
            outputStream.write(completeChar.getFirstValue());
            outputStream.flush();
            Thread.sleep(200);

            assertEquals("file", console.getBuffer());
            assertEquals("Display all 105 possibilities? (y or n)", getLastOutputLine(byteArrayOutputStream));

            outputStream.write("n".getBytes());
            outputStream.flush();

            Thread.sleep(200);

            assertEquals("file", console.getBuffer());
        } finally {
            console.stop();
        }
    }

    String getLastOutputLine(ByteArrayOutputStream os) {
        String output = new String(os.toByteArray());
        String[] lines = output.split("\n");
        return lines[lines.length - 1];
    }

    class CompletionConsoleCallback extends AeshConsoleCallback {
        private transient int count = 0;
        final Console console;
        final OutputStream outputStream;
        CompletionConsoleCallback(Console console, OutputStream outputStream) {
            this.outputStream = outputStream;
            this.console = console;
        }
        @Override
        public int execute(ConsoleOperation output) throws InterruptedException {
            if(count == 0) {
                assertEquals("foobar ", output.getBuffer());
            }
            else if(count == 1)
                assertEquals("barfoo", output.getBuffer());
            else if(count == 2) {
                assertEquals("less:", output.getBuffer());
                console.stop();
            }

            count++;
            return 0;
        }
    }

    class CompletionConsoleCallback2 extends AeshConsoleCallback {
        private int count = 0;
        Console console;
        CompletionConsoleCallback2(Console console) {
            this.console = console;
        }
        @Override
        public int execute(ConsoleOperation output) throws InterruptedException {
            if(count == 0) {
                assertEquals("less ", output.getBuffer());
            }

            count++;
            return 0;
        }
    }

    class CompletionConsoleCallback3 extends AeshConsoleCallback {
        private int count = 0;
        final Console console;
        final OutputStream outputStream;
        CompletionConsoleCallback3(Console console, OutputStream outputStream) {
            this.outputStream = outputStream;
            this.console = console;
        }
        @Override
        public int execute(ConsoleOperation output) throws InterruptedException {
            if(count == 0) {
                assertEquals("foo", output.getBuffer());
            }
            else if(count == 1)
                assertEquals("barfoo", output.getBuffer());
            else if(count == 2) {
                assertEquals("less:", output.getBuffer());
                console.stop();
            }

            count++;
            return 0;
        }
    }
    */

}
