/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.OptionList;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.builder.CommandBuilder;
import org.jboss.aesh.cl.builder.OptionBuilder;
import org.jboss.aesh.cl.completer.CompleterData;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.validator.OptionValidator;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.command.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandInvocation;
import org.jboss.aesh.console.command.CommandRegistry;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.ConsoleCommand;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.util.ANSI;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshExample {

    public static void main(String[] args) throws CommandLineParserException {

        ProcessedCommand fooCommand = new CommandBuilder()
                .name("foo")
                .description("fooing")
                .addOption(new OptionBuilder()
                        .name("bar")
                        .addDefaultValue("en 1 0")
                        .addDefaultValue("to 2 0")
                        .fieldName("bar")
                        .type(String.class)
                        .create())
                .generateParameter();

        Settings settings = new SettingsBuilder().logging(true).create();
        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ExitCommand.class)
                .command(fooCommand, FooCommand.class)
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
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException {
            commandInvocation.stop();
            return CommandResult.SUCCESS;
        }
    }

    //this command use a builder defined above to specify the meta data needed
    public static class FooCommand implements Command {

        private String bar;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException {
           if(bar == null)
               commandInvocation.getShell().out().println("NO BAR!");
            else
               commandInvocation.getShell().out().println("you set bar to: " + bar);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "testing")
    public static class TestConsoleCommand implements Command, ConsoleCommand {

        @Option(hasValue = false)
        private boolean bar;

        private boolean attached = true;
        private Shell shell;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException {
            this.shell = commandInvocation.getShell();
            commandInvocation.attachConsoleCommand(this);
            display();

            return CommandResult.SUCCESS;
        }

        private void display() {
            shell.out().print(ANSI.getAlternateBufferScreen());
            shell.out().print("press q to stop.....");
            shell.out().flush();
        }

        private void stop() {
            shell.out().print(ANSI.getMainBufferScreen());
            attached = false;
        }

        @Override
        public void processOperation(Operation operation) throws IOException {
            if(operation.getInput()[0] == 'q') {
                stop();
            }
            else {
                shell.out().print((char) operation.getInput()[0]);
                shell.out().flush();
            }
        }

        @Override
        public boolean isAttached() {
            return attached;
        }
    }

    @CommandDefinition(name="ls", description = "[OPTION]... [FILE]...")
    public static class LsCommand implements Command {

        @Option(hasValue = false, description = "set foo to true/false")
        private Boolean foo;

        @Option(hasValue = false, description = "set the bar")
        private boolean bar;

        @Option(completer = LessCompleter.class, defaultValue = {"MORE"}, argument = "SIZE")
        private String less;

        @OptionList(defaultValue = "/tmp", description = "file location", valueSeparator = ':',
                validator = DirectoryValidator.class,
                activator = BarActivator.class)
        List<File> files;

        @Option(hasValue = false, description = "display this help and exit")
        private boolean help;

        @Arguments
        private List<File> arguments;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException {
            if(help) {
                commandInvocation.getShell().out().println(commandInvocation.getHelpInfo("ls"));
            }
            else {
                if(foo)
                    commandInvocation.getShell().out().println("you set foo to: " + foo);
                if(bar)
                    commandInvocation.getShell().out().println("you set bar to: " + bar);
                if(less != null)
                    commandInvocation.getShell().out().println("you set less to: " + less);
                if(files != null)
                    commandInvocation.getShell().out().println("you set file to: " + files);

                if(arguments != null) {
                    for(File f : arguments)
                        commandInvocation.getShell().out().println(f.toString());
                }
            }
            return CommandResult.SUCCESS;
        }
    }

    public static class LessCompleter implements OptionCompleter {

        @Override
        public void complete(CompleterData completerData) {
            List<String> completeList = new ArrayList<String>();
            if(completerData.getGivenCompleteValue() == null || completerData.getGivenCompleteValue().length() == 0)
                completeList.add("1");
            else {
                char lastChar = completerData.getGivenCompleteValue().charAt(completerData.getGivenCompleteValue().length()-1);
                if(Character.isDigit(lastChar)) {
                    int i = (int) lastChar;
                    i++;
                    completeList.add(completerData.getGivenCompleteValue()+i);
                }
            }
        }
    }

    public static class DirectoryValidator implements OptionValidator<File> {
        @Override
        public void validate(File value) throws OptionValidatorException {
            if(!value.isDirectory()) {
                throw new OptionValidatorException("File validation failed, must be a directory.");
            }
        }
    }

    public static class BarActivator implements OptionActivator {

        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            ProcessedOption bar = processedCommand.findLongOption("bar");
            if(bar != null && bar.getValue() != null)
                return true;
            else
                return false;
        }
    }

}
