/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.completion;

import org.jboss.aesh.cl.CommandLineParser;
import org.jboss.aesh.cl.OptionBuilder;
import org.jboss.aesh.cl.ParameterBuilder;
import org.jboss.aesh.cl.internal.OptionInt;
import org.jboss.aesh.cl.internal.ParameterInt;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOutput;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.edit.KeyOperation;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CompletionConsoleTest extends BaseConsoleTest {

    private KeyOperation completeChar =  new KeyOperation(Key.CTRL_I, Operation.COMPLETE);

    public CompletionConsoleTest(String test) {
        super(test);
    }

    public void testCompletion() throws IOException {
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        Console console = getTestConsole(pipedInputStream);

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

        outputStream.write("foo".getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.write("\n".getBytes());
        ConsoleOutput output = console.read("");
        assertEquals("foobar ", output.getBuffer());

        outputStream.write("bar".getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.write("\n".getBytes());
        output = console.read("");
        assertEquals("barfoo", output.getBuffer());

        outputStream.write("le".getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.write("\n".getBytes());
        output = console.read("");
        assertEquals("less:", output.getBuffer());

        console.stop();
    }

    public void testCompletionWithOptions() throws IOException {

        final ParameterInt param = new ParameterBuilder().name("less")
                .usage("less -options <files>")
                .generateParameter();

        param.addOption(new OptionBuilder().name('f').longName("foo").hasValue(true).create());
        param.addOption(new OptionBuilder().name('b').longName("bar").hasValue(true).create());

        final CommandLineParser parser = new CommandLineParser(param);
        final StringBuilder builder = new StringBuilder();

        Completion completion = new Completion() {
            @Override
            public void complete(CompleteOperation co) {
                if(parser.getParameters().get(0).getName().startsWith(co.getBuffer())) {
                    co.addCompletionCandidate(parser.getParameters().get(0).getName());
                }
                // commandline longer than the name
                else if(co.getBuffer().startsWith(parser.getParameters().get(0).getName())){
                   if(co.getBuffer().length() > parser.getParameters().get(0).getName().length())  {
                      if(co.getBuffer().endsWith(" --")) {
                         for(OptionInt o : parser.getParameters().get(0).getOptions()) {
                             co.addCompletionCandidate("--"+o.getLongName());
                             builder.append("--"+o.getLongName()+" ");
                         }
                      }
                      else if(co.getBuffer().endsWith(" -")) {
                          for(OptionInt o : parser.getParameters().get(0).getOptions()) {
                              co.addCompletionCandidate("-"+o.getName());
                              builder.append("-"+o.getName()+" ");
                          }
                      }
                   }
                }
            }
        };
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

        Console console = getTestConsole(pipedInputStream);
        console.addCompletion(completion);


        outputStream.write("le".getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.write("\n".getBytes());
        ConsoleOutput output = console.read("");
        assertEquals("less ", output.getBuffer());

        outputStream.write("less -".getBytes());
        outputStream.write(completeChar.getFirstValue());
        outputStream.write("\n".getBytes());
        output = console.read("");
        assertEquals("-f -b ", builder.toString());

    }


}
