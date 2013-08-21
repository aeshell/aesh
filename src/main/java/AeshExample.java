/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.CompleterData;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Command;
import org.jboss.aesh.console.CommandResult;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshExample {

    public static void main(String[] args) {

        Settings settings = new SettingsBuilder().logging(true).create();
        AeshConsole aeshConsole = new AeshConsoleBuilder().settings(settings)
                .prompt(new Prompt("[aesh@rules]$ "))
                .command(ExitCommand.class)
                .command(FooCommand.class)
                .command(LsCommand.class)
                .create();

        aeshConsole.start();

    }


    @CommandDefinition(name="exit", description = "exit the program")
    public static class ExitCommand implements Command {

        @Override
        public CommandResult execute(AeshConsole console) throws IOException {
            console.stop();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="foo", description = "fooing")
    public static class FooCommand implements Command {

        @Option(defaultValue = {"en","to"})
        private String bar;

        @Override
        public CommandResult execute(AeshConsole console) throws IOException {
           if(bar == null)
               console.out().println("NO BAR!");
            else
               console.out().println("you set bar to: " + bar);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="ls", description = "fooing")
    public static class LsCommand implements Command {

        @Option
        private Boolean foo;

        @Option(completer = LessCompleter.class)
        private String less;

        @Option
        File file;

        @Arguments
        private List<File> files;

        @Override
        public CommandResult execute(AeshConsole console) throws IOException {
           if(foo == null)
               console.out().println("NO FOO!");
            else
               console.out().println("you set foo to: " + foo);

            if(files != null) {

            }
            return CommandResult.SUCCESS;
        }
    }

    public static class LessCompleter implements OptionCompleter {

        @Override
        public CompleterData complete(String completeValue) {
            List<String> completeList = new ArrayList<String>();
            if(completeValue == null || completeValue.length() == 0)
                completeList.add("1");
            else {
                char lastChar = completeValue.charAt(completeValue.length()-1);
                if(Character.isDigit(lastChar)) {
                    int i = (int) lastChar;
                    i++;
                    completeList.add(completeValue+i);
                }
            }
            return new CompleterData(completeList);
        }
    }

}
