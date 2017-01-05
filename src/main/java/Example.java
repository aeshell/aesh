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
import org.aesh.command.option.Arguments;
import org.aesh.command.CommandDefinition;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.builder.CommandBuilder;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.impl.parser.CommandLineParserException;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.renderer.OptionRenderer;
import org.aesh.command.validator.OptionValidator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.console.AeshContext;
import org.aesh.command.Shell;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.Command;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.CommandResult;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.command.validator.ValidatorInvocationProvider;
import org.aesh.console.helper.ManProvider;
import org.aesh.console.settings.Settings;
import org.aesh.console.settings.SettingsBuilder;
import org.aesh.io.FileResource;
import org.aesh.io.Resource;
import org.aesh.readline.Prompt;
import org.aesh.readline.ReadlineConsole;
import org.aesh.readline.action.KeyAction;
import org.aesh.terminal.Key;
import org.aesh.terminal.formatting.CharacterType;
import org.aesh.terminal.formatting.Color;
import org.aesh.terminal.formatting.TerminalColor;
import org.aesh.terminal.formatting.TerminalString;
import org.aesh.terminal.formatting.TerminalTextStyle;
import org.aesh.util.ANSI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.aesh.command.CommandException;
import org.aesh.util.Config;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Example {

    public static void main(String[] args) throws CommandLineParserException, IOException {

        CommandBuilder fooCommand = new CommandBuilder()
                .name("foo")
                .description("fooing")
                .addOption(new ProcessedOptionBuilder()
                        .name("bar")
                        .addDefaultValue("en 1 0")
                        .addDefaultValue("to 2 0")
                        .fieldName("bar")
                        .type(String.class)
                        .renderer(new BlueBoldRenderer())
                        .create())
                .addOption(new ProcessedOptionBuilder()
                        .name("foo")
                        .fieldName("foo")
                        .type(String.class)
                        .create())
                .command(FooCommand.class);

                /*
        ProcessedCommand fooCommand = new ProcessedCommandBuilder()
                .name("foo")
                .description("fooing")
                .addOption(new ProcessedOptionBuilder()
                        .name("bar")
                        .addDefaultValue("en 1 0")
                        .addDefaultValue("to 2 0")
                        .fieldName("bar")
                        .type(String.class)
                        .renderer(new BlueBoldRenderer())
                        .create())
                .addOption(new ProcessedOptionBuilder()
                        .name("foo")
                        .fieldName("foo")
                        .type(String.class)
                        .create())
                .create();
                */

        SettingsBuilder builder = new SettingsBuilder()
                .logging(true)
                .enableMan(true)
                .enableAlias(true)
                .enableExport(true)
                .setExecuteFileAtStart(new FileResource(
                        Config.getHomeDir()+Config.getPathSeparator()+".aeshrc"))
                .readInputrc(false);
                /*
                .interruptHook(new InterruptHook() {
                    @Override
                    public void handleInterrupt(Console console) {
                        console.getShell().out().println("^C");
                        console.clearBufferAndDisplayPrompt();
                    }
                });
                */
       CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ExitCommand.class)
                .command(fooCommand.create())
                .command(HiddenCommand.class)
                .command(LsCommand.class)
                .command(TestConsoleCommand.class)
                .command(PromptCommand.class)
                .command(RunCommand.class)
                .command(GroupCommand.class)
                //example on how to create a command with a simple lambda
                .command(new CommandBuilder().name("quit").command(commandInvocation -> {
                    commandInvocation.stop();
                    return CommandResult.SUCCESS;
                }).create())
                .create();

        Settings settings = builder
                .commandRegistry(registry)
                .manProvider(new ManProviderExample())
                .validatorInvocationProvider(new ExampleValidatorInvocationProvider())
                .create();

        ReadlineConsole console = new ReadlineConsole(settings);
        console.setPrompt(new Prompt(new TerminalString("[aesh@rules]$ ",
                        new TerminalColor(Color.GREEN, Color.DEFAULT, Color.Intensity.BRIGHT))));

        console.start();
    }

    @CommandDefinition(name = "exit", description = "exit the program", aliases = {"quit"})
    public static class ExitCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.stop();
            return CommandResult.SUCCESS;
        }
    }

    private static class HideActivator implements CommandActivator {
        @Override
        public boolean isActivated(ProcessedCommand command) {
            return false;
        }
    }

    @CommandDefinition(name = "hidden", description = "hidden command", activator = HideActivator.class)
    public static class HiddenCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.print("Command exists but is not shown");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "run", description = "")
    public static class RunCommand implements Command {

        @Arguments
        private List<Resource> arguments;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            commandInvocation.putProcessInBackground();

            if (arguments != null && arguments.size() > 0 && arguments.get(0).isLeaf()) {
                try {
                    List<String> script = readScriptFile(arguments.get(0));

                    for (String line : script) {
                        commandInvocation.executeCommand(line + Config.getLineSeparator());
                    }
                } catch (Exception ex) {
                    throw new CommandException(ex);
                }
            }

            return CommandResult.SUCCESS;
        }

        private List<String> readScriptFile(Resource resource) throws IOException {
            List<String> lines = new ArrayList<>();
            BufferedReader br = new BufferedReader(new InputStreamReader(resource.read()));
            String line = br.readLine();
            while (line != null) {
                if (line.trim().length() > 0 && !line.trim().startsWith("#"))
                    lines.add(line);
                line = br.readLine();
            }

            return lines;
        }
    }


    //this command use a builder defined above to specify the meta data needed
    public static class FooCommand implements Command {

        private String bar;

        private String foo;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
           if(bar == null)
               commandInvocation.getShell().write("NO BAR!");
            else {
               commandInvocation.getShell().write("you set bar to: " + bar);
               commandInvocation.getShell().write("lets work a bit...... ");
               Thread.sleep(2000);
           }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "testing")
    public static class TestConsoleCommand implements Command {

        @Option(hasValue = false, required = true)
        private boolean bar;

        @Option(hasValue = false)
        private boolean barbar;

        @Option(overrideRequired = true, hasValue = false)
        private boolean help;

        @Option(hasValue = false)
        private boolean helpPlease;

        private Shell shell;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            this.shell = commandInvocation.getShell();
            if(help) {
                shell.write(commandInvocation.getHelpInfo("test"));
            }
            else {
                try {
                    //display();
                    processOperation(commandInvocation);
                } catch (IOException ex) {
                    throw new CommandException(ex);
                }
            }

            return CommandResult.SUCCESS;
        }

        private void display() {
            shell.write(ANSI.ALTERNATE_BUFFER);
        }

        private void stop() {
            shell.write(ANSI.MAIN_BUFFER);
        }

        public void processOperation(CommandInvocation invocation) throws IOException, InterruptedException {
            //first ask for username, then password
            String username = promptForUsername(invocation);
            String password = promptForInput("password: ", '*', invocation);

            shell.write("we got username: " + username + ", password: " + password);
        }

        private String promptForUsername(CommandInvocation invocation) throws InterruptedException {
            invocation.print("username: ");
            return invocation.inputLine();
        }

        private String promptForInput(String prompt, Character mask,
                                      CommandInvocation invocation) throws IOException, InterruptedException {
            return invocation.inputLine(new Prompt(prompt, mask));
        }
    }

    @CommandDefinition(name="ls", description = "[OPTION]... [FILE]...")
    public static class LsCommand implements Command {

        @Option(shortName = 'f', hasValue = false, description = "set foo to true/false")
        private Boolean foo;

        @Option(hasValue = false, description = "set the bar", renderer = BlueBoldRenderer.class)
        private boolean bar;

        @Option(shortName = 'l', completer = LessCompleter.class, defaultValue = {"MORE"}, argument = "SIZE")
        private String less;

        @OptionList(defaultValue = "/tmp", description = "file location", valueSeparator = ':',
                validator = DirectoryValidator.class,
                activator = BarActivator.class)
        List<File> files;

        @Option(hasValue = false, description = "display this help and exit")
        private boolean help;

        @Arguments(description = "files or directories thats listed")
        private List<Resource> arguments;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if(help) {
                commandInvocation.getShell().write(commandInvocation.getHelpInfo("ls"));
            }
            else {
                if(foo)
                    commandInvocation.getShell().write("you set foo to: " + foo);
                if(bar)
                    commandInvocation.getShell().write("you set bar to: " + bar);
                if(less != null)
                    commandInvocation.getShell().write("you set less to: " + less);
                if(files != null)
                    commandInvocation.getShell().write("you set file to: " + files);

                if(arguments != null) {
                    for(Resource f : arguments)
                        commandInvocation.getShell().write(f.toString());
                }
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "prompt", description = "")
    public static class PromptCommand implements Command {

        @Option(hasValue = false)
        private boolean bar;

        private Shell shell;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            this.shell = commandInvocation.getShell();
            if(bar) {
                shell.write("are you sure you want bar? (y/n) ");
                KeyAction operation = null;
                try {
                    operation = commandInvocation.input();
                }
                catch (InterruptedException e) {
                    return CommandResult.FAILURE;
                }
                if(operation instanceof Key)
                    processOperation((Key) operation);
            }
            return CommandResult.SUCCESS;
        }

        public void processOperation(Key operation) {
            if(operation == Key.y) {
                shell.write(Config.getLineSeparator()+"you wanted bar!");
            }
            else
                shell.write(Config.getLineSeparator()+"you chickened out!!");
        }

    }

    public static class LessCompleter implements OptionCompleter {

        @Override
        public void complete(CompleterInvocation completerData) {
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

    public static class DirectoryValidator implements OptionValidator<DirectoryValidatorInvocation> {
        @Override
        public void validate(DirectoryValidatorInvocation validatorInvocation) throws OptionValidatorException {
            if(!validatorInvocation.getValue().isDirectory())
                throw new OptionValidatorException("File validation failed, must be a directory.");
        }
    }

    public static class DirectoryValidatorInvocation implements ValidatorInvocation<File, Command> {

        private final File file;
        private final Command command;
        private final AeshContext aeshContext;

        public DirectoryValidatorInvocation(File file, Command command, AeshContext aeshContext) {
            this.file = file;
            this.command = command;
            this.aeshContext = aeshContext;
        }

        @Override
        public File getValue() {
            return file;
        }

        @Override
        public Command getCommand() {
            return command;
        }

        @Override
        public AeshContext getAeshContext() {
            return aeshContext;
        }
    }

    public static class ExampleValidatorInvocationProvider implements ValidatorInvocationProvider<ValidatorInvocation<File, Command>> {

        @Override
        public ValidatorInvocation<File, Command> enhanceValidatorInvocation(ValidatorInvocation validatorInvocation) {
            if(validatorInvocation.getValue() instanceof File)
                return new DirectoryValidatorInvocation( (File) validatorInvocation.getValue(),
                        (Command) validatorInvocation.getCommand(), validatorInvocation.getAeshContext());
            else
                return validatorInvocation;
        }
    }

    public static class BarActivator implements OptionActivator {

        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            ProcessedOption bar = processedCommand.findLongOption("bar");
            return bar != null && bar.getValue() != null;
        }
    }

    public static class BlueBoldRenderer implements OptionRenderer {

        private static final TerminalTextStyle style = new TerminalTextStyle(CharacterType.UNDERLINE);
        private static final TerminalColor color = new TerminalColor(42, Color.DEFAULT);

        @Override
        public TerminalColor getColor() {
            return color;
        }

        @Override
        public TerminalTextStyle getTextType() {
            return style;
        }
    }

    public static class ManProviderExample implements ManProvider {

        @Override
        public InputStream getManualDocument(String commandName) {
            //this is just a stupid example always returning a file located in /tmp
            try {
                return new FileInputStream("/tmp/asciitest2.txt");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @GroupCommandDefinition(name = "group", description = "This is a group command",
            groupCommands = {Child1.class, Child2.class})
    public static class GroupCommand implements Command {

        @Option(hasValue = false, description = "display this help option")
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if(help)
                commandInvocation.getShell().write(commandInvocation.getHelpInfo("group"));
            else
                commandInvocation.getShell().write("only executed group, it doesnt do much...");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child1", description = "")
    public static class Child1 implements Command {

        @Option(description = "set foo")
        private String foo;
        @Option(hasValue = false, description = "display this help option")
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if(help)
                commandInvocation.getShell().write(commandInvocation.getHelpInfo("group child1"));
            else
                commandInvocation.getShell().write("foo is set to: "+foo);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child2", description = "")
    public static class Child2 implements Command {
        @Option
        private boolean bar;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.getShell().write("bar is set to: "+bar);
            return CommandResult.SUCCESS;
        }
    }
}
