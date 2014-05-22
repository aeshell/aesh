/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.completion;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import org.jboss.aesh.cl.builder.CommandBuilder;
import org.jboss.aesh.cl.builder.OptionBuilder;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.parser.AeshCommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.complete.CompletionRegistration;
import org.jboss.aesh.console.AeshConsoleCallback;
import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.KeyOperation;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.TestTerminal;
import org.junit.Test;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CompletionConsoleTest extends BaseConsoleTest {

    private KeyOperation completeChar =  new KeyOperation(Key.CTRL_I, Operation.COMPLETE);

    private static final byte[] LINE_SEPARATOR = Config.getLineSeparator().getBytes();

    @Test
    public void completion() throws Exception {
        invokeTestConsole(3, new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws IOException {
                Completion completion = new Completion() {
                    @Override
                    public void complete(CompleteOperation co) {
                        if(co.getBuffer().equals("foo"))
                            co.addCompletionCandidate("foobar");
                    }
                };
                console.addCompletion(completion);


                Completion completion2 = new Completion() {
                    @Override
                    public void complete(CompleteOperation co) {
                        if(co.getBuffer().equals("bar")) {
                            co.addCompletionCandidate("barfoo");
                            co.doAppendSeparator(false);
                        }
                    }
                };
                console.addCompletion(completion2);

                Completion completion3 = new Completion() {
                    @Override
                    public void complete(CompleteOperation co) {
                        if(co.getBuffer().equals("le")) {
                            co.addCompletionCandidate("less");
                            co.setSeparator(':');
                        }
                    }
                };
                console.addCompletion(completion3);

                out.write("foo".getBytes());
                out.write(completeChar.getFirstValue());
                out.write(LINE_SEPARATOR);
                out.flush();

                out.write("bar".getBytes());
                out.write(completeChar.getFirstValue());
                out.write(LINE_SEPARATOR);
                out.flush();

                out.write("le".getBytes());
                out.write(completeChar.getFirstValue());
                out.write(LINE_SEPARATOR);
                out.flush();
            }
        }, new Verify() {
           private int count = 0;
           @Override
           public int call(Console console, ConsoleOperation op) {
               if(count == 0) {
                   assertEquals("foobar ", op.getBuffer());
               }
               else if(count == 1)
                   assertEquals("barfoo", op.getBuffer());
               else if(count == 2) {
                   assertEquals("less:", op.getBuffer());
               }
               count++;
               return 0;
           }
        });
    }

    @Test
    public void removeCompletion() throws Exception {
        invokeTestConsole(3, new Setup() {
            @Override
            public void call(Console console, OutputStream out) throws IOException {
                Completion completion = new Completion() {
                    @Override
                    public void complete(CompleteOperation co) {
                        if(co.getBuffer().equals("foo"))
                            co.addCompletionCandidate("foobar");
                    }
                };
                CompletionRegistration completionRegistration = console.addCompletion(completion);

                Completion completion2 = new Completion() {
                    @Override
                    public void complete(CompleteOperation co) {
                        if(co.getBuffer().equals("bar")) {
                            co.addCompletionCandidate("barfoo");
                            co.doAppendSeparator(false);
                        }
                    }
                };
                console.addCompletion(completion2);

                Completion completion3 = new Completion() {
                    @Override
                    public void complete(CompleteOperation co) {
                        if(co.getBuffer().equals("le")) {
                            co.addCompletionCandidate("less");
                            co.setSeparator(':');
                        }
                    }
                };
                console.addCompletion(completion3);
                completionRegistration.removeCompletion();

                out.write("foo".getBytes());
                out.write(completeChar.getFirstValue());
                out.write(LINE_SEPARATOR);
                out.flush();

                out.write("bar".getBytes());
                out.write(completeChar.getFirstValue());
                out.write(LINE_SEPARATOR);
                out.flush();

                out.write("le".getBytes());
                out.write(completeChar.getFirstValue());
                out.write(LINE_SEPARATOR);
                out.flush();
            }
        }, new Verify() {
           private int count = 0;
           @Override
           public int call(Console console, ConsoleOperation op) {
               if(count == 0) {
                   assertEquals("foo", op.getBuffer());
               }
               else if(count == 1)
                   assertEquals("barfoo", op.getBuffer());
               else if(count == 2) {
                   assertEquals("less:", op.getBuffer());
               }
               count++;
               return 0;
           }
        });
    }

    @Test
    public void completionWithOptions() throws IOException, InterruptedException, CommandLineParserException {

        final ProcessedCommand param = new CommandBuilder().name("less")
                .description("less -options <files>")
                .generateCommand();

        param.addOption(new OptionBuilder().shortName('f').name("foo").hasValue(true).type(String.class).create());

        final CommandLineParser parser = new AeshCommandLineParser(param);
        final StringBuilder builder = new StringBuilder();

        Completion completion = new Completion() {
            @Override
            public void complete(CompleteOperation co) {
                if(parser.getCommand().getName().startsWith(co.getBuffer())) {
                    co.addCompletionCandidate(parser.getCommand().getName());
                }
                // commandline longer than the name
                else if(co.getBuffer().startsWith(parser.getCommand().getName())){
                   if(co.getBuffer().length() > parser.getCommand().getName().length())  {
                      if(co.getBuffer().endsWith(" --")) {
                         for(ProcessedOption o : parser.getCommand().getOptions()) {
                             co.addCompletionCandidate("--"+o.getName());
                             builder.append(o.getName()+" ");
                         }
                          co.setOffset(co.getOffset());
                      }
                      else if(co.getBuffer().endsWith(" -")) {
                          for(ProcessedOption o : parser.getCommand().getOptions()) {
                              co.addCompletionCandidate("-"+o.getShortName());
                              builder.append("-"+o.getShortName()+" ");
                          }
                      }
                   }
                }
            }
        };
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Settings settings = new SettingsBuilder()
                .terminal(new TestTerminal())
                .inputStream(pipedInputStream)
                .outputStream(new PrintStream(byteArrayOutputStream))
                .logging(true)
                .create();

        Console console = new Console(settings);

        console.addCompletion(completion);

        console.setConsoleCallback(new CompletionConsoleCallback2(console));
        console.start();

        outputStream.write("le".getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();
        Thread.sleep(150);

        assertEquals("less ", console.getBuffer());

        outputStream.write(LINE_SEPARATOR);
        outputStream.write("less --".getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.flush();

        Thread.sleep(200);

        console.stop();
    }

    class CompletionConsoleCallback extends AeshConsoleCallback {
        private transient int count = 0;
        Console console;
        OutputStream outputStream;
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
        Console console;
        OutputStream outputStream;
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

}
