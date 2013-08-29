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
import org.jboss.aesh.console.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Command;
import org.jboss.aesh.console.CommandRegistry;
import org.jboss.aesh.console.CommandResult;
import org.jboss.aesh.console.ConsoleCommand;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.operator.ControlOperator;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.util.ANSI;

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
        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ExitCommand.class)
                .command(FooCommand.class)
                .command(LsCommand.class)
                .command(TestConsoleCommand.class)
                .create();
        AeshConsole aeshConsole = new AeshConsoleBuilder()
                .commandRegistry(registry)
                .settings(settings)
                .prompt(new Prompt("[aesh@rules]$ "))
                .create();

        aeshConsole.start();
    }

    @CommandDefinition(name="exit", description = "exit the program")
    public static class ExitCommand implements Command {

        @Override
        public CommandResult execute(AeshConsole console,
                                     ControlOperator operator) throws IOException {
            console.stop();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="foo", description = "fooing")
    public static class FooCommand implements Command {

        @Option(defaultValue = {"en","to"})
        private String bar;

        @Override
        public CommandResult execute(AeshConsole console,
                                     ControlOperator operator) throws IOException {
           if(bar == null)
               console.out().println("NO BAR!");
            else
               console.out().println("you set bar to: " + bar);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "testing")
    public static class TestConsoleCommand implements Command, ConsoleCommand {

        private boolean attached = true;
        private AeshConsole console;

        @Override
        public CommandResult execute(AeshConsole aeshConsole, ControlOperator operator) throws IOException {
            this.console = aeshConsole;
            console.attachConsoleCommand(this);
            display();

            return CommandResult.SUCCESS;
        }

        private void display() {
            console.out().print(ANSI.getAlternateBufferScreen());
            console.out().print("press q to stop.....");
            console.out().flush();
        }

        private void stop() {
            console.out().print(ANSI.getMainBufferScreen());
            attached = false;
        }

        @Override
        public void processOperation(Operation operation) throws IOException {
            if(operation.getInput()[0] == 'q') {
                stop();
            }
            else {
                console.out().print((char) operation.getInput()[0]);
                console.out().flush();
            }
        }

        @Override
        public boolean isAttached() {
            return attached;
        }
    }

    @CommandDefinition(name="ls", description = "fooing")
    public static class LsCommand implements Command {

        @Option(hasValue = false)
        private Boolean foo;

        @Option(completer = LessCompleter.class)
        private String less;

        @Option
        File file;

        @Arguments
        private List<File> files;

        @Override
        public CommandResult execute(AeshConsole console,
                                     ControlOperator operator) throws IOException {
           if(foo != null)
               console.out().println("you set foo to: " + foo);

            if(files != null) {
                for(File f : files)
                    console.out().println(f.toString());

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
