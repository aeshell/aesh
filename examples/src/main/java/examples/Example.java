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
package examples;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.builder.CommandBuilder;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.converter.ConverterInvocation;
import org.aesh.command.impl.internal.ParsedCommand;
import org.aesh.command.impl.internal.ParsedOption;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.renderer.OptionRenderer;
import org.aesh.command.settings.ManProvider;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.shell.Shell;
import org.aesh.command.validator.OptionValidator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.validator.ValidatorInvocation;
import org.aesh.command.validator.ValidatorInvocationProvider;
import org.aesh.io.FileResource;
import org.aesh.io.Resource;
import org.aesh.readline.AeshContext;
import org.aesh.readline.Prompt;
import org.aesh.readline.ReadlineConsole;
import org.aesh.readline.action.KeyAction;
import org.aesh.readline.terminal.Key;
import org.aesh.readline.terminal.formatting.CharacterType;
import org.aesh.readline.terminal.formatting.Color;
import org.aesh.readline.terminal.formatting.TerminalColor;
import org.aesh.readline.terminal.formatting.TerminalString;
import org.aesh.readline.terminal.formatting.TerminalTextStyle;
import org.aesh.selector.Selector;
import org.aesh.selector.SelectorType;
import org.aesh.terminal.utils.ANSI;
import org.aesh.terminal.utils.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Example {

    public static void main(String[] args) throws CommandLineParserException, IOException, CommandRegistryException {

        CommandBuilder<FooCommand> fooCommand = CommandBuilder.<FooCommand>builder()
                .name("foo")
                .description("fooing")
                .addOption(ProcessedOptionBuilder.builder()
                        .name("bar")
                        .addDefaultValue("en 1 0")
                        .addDefaultValue("to 2 0")
                        .fieldName("bar")
                        .type(String.class)
                        .renderer(new BlueBoldRenderer())
                        .build())
                .addOption(ProcessedOptionBuilder.builder()
                        .name("foo")
                        .fieldName("foo")
                        .type(String.class)
                        .build())
                .command(FooCommand.class);

        CommandRegistry registry = AeshCommandRegistryBuilder.builder()
                .command(ExitCommand.class)
                .command(fooCommand.create())
                .command(HiddenCommand.class)
                .command(LsCommand.class)
                .command(TestConsoleCommand.class)
                .command(PromptCommand.class)
                .command(RunCommand.class)
                .command(AskCommand.class)
                .command(ClearCommand.class)
                .command(GroupCommand.class)
                .command(LongOutputCommand.class)
                .command(ReadlineCommand.class)
                .command(SelectCommand.class)
                //example on how to build a command with a simple lambda
                .command(CommandBuilder.builder().name("quit").command(commandInvocation -> {
                    commandInvocation.stop();
                    return CommandResult.SUCCESS;
                }).create())
                .create();


        SettingsBuilder<CommandInvocation, ConverterInvocation, CompleterInvocation, ValidatorInvocation,
                               OptionActivator, CommandActivator> builder =
                SettingsBuilder.builder()
                        .logging(true)
                        .enableMan(true)
                        .enableAlias(true)
                        .enableExport(true)
                        .enableSearchInPaging(true)
                        .setExecuteFileAtStart(new FileResource(
                                Config.getHomeDir()+Config.getPathSeparator()+".aeshrc"))
                        .readInputrc(false)
                        .commandRegistry(registry)
                        .manProvider(new ManProviderExample())
                        .validatorInvocationProvider(new ExampleValidatorInvocationProvider());

        ReadlineConsole console = new ReadlineConsole(builder.build());
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
        public boolean isActivated(ParsedCommand command) {
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

        @Arguments(required = true)
        private List<Resource> arguments;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

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
    public static class FooCommand implements Command<CommandInvocation> {

        private String bar;

        private String foo;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
           if(bar == null)
               commandInvocation.getShell().writeln("NO BAR!");
            else {
               commandInvocation.getShell().writeln("you set bar to: " + bar);
               commandInvocation.getShell().writeln("lets work a bit...... ");
               Thread.sleep(2000);
           }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "test", description = "testing")
    public static class TestConsoleCommand implements Command {

        @Option(hasValue = false, required = true)
        private boolean verbose;

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
                shell.writeln(commandInvocation.getHelpInfo("test"));
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

            shell.writeln("we got username: " + username + ", password: " + password);
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

    @CommandDefinition(name="ls", description = "[OPTION]... [FILE]...", version = "1.0")
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
                commandInvocation.getShell().writeln(commandInvocation.getHelpInfo("ls"));
            }
            else {
                if(foo)
                    commandInvocation.getShell().writeln("you set foo to: " + foo);
                if(bar)
                    commandInvocation.getShell().writeln("you set bar to: " + bar);
                if(less != null)
                    commandInvocation.getShell().writeln("you set less to: " + less);
                if(files != null)
                    commandInvocation.getShell().writeln("you set file to: " + files);

                if(arguments != null) {
                    for(Resource f : arguments)
                        commandInvocation.getShell().writeln(f.toString());
                }
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "ask", description = "")
    public static class AskCommand implements Command {

        @Option(askIfNotSet = true)
        private String path;

        @Arguments(askIfNotSet = true)
        private List<String> args;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

            commandInvocation.println("path is set to: "+path);
            if(args != null && args.size() > 0)
            commandInvocation.println("args[0] is: "+args.get(0));

            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "clear", description = "")
    public static class ClearCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.getShell().clear();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "long-output", description = "")
    public static class LongOutputCommand implements Command {

        @Option(hasValue = false)
        private boolean prompt;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < commandInvocation.getShell().size().getHeight() * 2; i++) {
                builder.append("A sentence to say " + i + " time how long this content is." + Config.getLineSeparator());
            }
            commandInvocation.getShell().writeln(builder.toString(), true);
            if (prompt) {
                commandInvocation.inputLine(new Prompt("What was the name of your first pet?: "));
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
                shell.writeln(Config.getLineSeparator()+"you wanted bar!");
            }
            else
                shell.writeln(Config.getLineSeparator()+"you chickened out!!");
        }

    }

    @CommandDefinition(name = "select", description = "The select command", generateHelp = true)
    public static class SelectCommand implements Command {

        @Option(selector = SelectorType.SELECT, description = "Choose your color",
                defaultValue = {"red", "green", "blue"})
        private String color;


        @Option(selector = SelectorType.SELECT, description = "Set to true or false")
        private boolean bool;

        private Shell shell;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            this.shell = commandInvocation.getShell();

            commandInvocation.println("Color: "+color+", Bool: "+bool);

            List<String> selected = new Selector(SelectorType.SELECT, new String[]{"blue","green", "red" },
                    "Which color do you want? ")
                    .doSelect(commandInvocation.getShell());

            if(selected.size() > 0)
                commandInvocation.println("You selected: "+selected.get(0));

            return CommandResult.SUCCESS;
        }

    }

    @CommandDefinition(name = "readline", description = "")
    public static class ReadlineCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            String msg;
            try {
                msg = commandInvocation.inputLine(new Prompt("Type something:"));
            } catch (InterruptedException e) {
                commandInvocation.println("Interrupted!");
                return CommandResult.FAILURE;
            }
            if (msg == null) {
                commandInvocation.println("Exit expected, leaving");
            } else {
                commandInvocation.println("Typed message:" + msg);
            }
            return CommandResult.SUCCESS;
        }
    }

    public static class LessCompleter implements OptionCompleter {

        @Override
        public void complete(CompleterInvocation completerData) {
            List<String> completeList = new ArrayList<>();
            if(completerData.getGivenCompleteValue() == null || completerData.getGivenCompleteValue().length() == 0) {
                completeList.add("1");
            }
            else {
                char lastChar = completerData.getGivenCompleteValue().charAt(completerData.getGivenCompleteValue().length()-1);
                if(Character.isDigit(lastChar)) {
                    int i = (int) lastChar;
                    i++;
                    completeList.add(completerData.getGivenCompleteValue()+i);
                }
            }
            completerData.addAllCompleterValues(completeList);
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
        public boolean isActivated(ParsedCommand parsedCommand) {
            ParsedOption bar = parsedCommand.findLongOption("bar");
            return bar != null && bar.value() != null;
        }
    }

    public static class BlueBoldRenderer implements OptionRenderer {

        private static final TerminalTextStyle style = new TerminalTextStyle(CharacterType.UNDERLINE);
        private static final TerminalColor color = new TerminalColor(42, Color.BLUE);

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
            groupCommands = {Base.class, Rebase.class})
    public static class GroupCommand implements Command {

        @Option(hasValue = false, description = "display this help option")
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if(help)
                commandInvocation.getShell().writeln(commandInvocation.getHelpInfo());
            else
                commandInvocation.getShell().writeln("only executed group, it doesnt do much...");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "base", description = "description of base command")
    public static class Base implements Command {

        @Option(description = "set foo")
        private String foo;
        @Option(hasValue = false, description = "display this help option")
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            if(help)
                commandInvocation.getShell().writeln(commandInvocation.getHelpInfo());
            else
                commandInvocation.getShell().writeln("foo is set to: "+foo);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "rebase", description = "description of rebase command")
    public static class Rebase implements Command {
        @Option
        private boolean bar;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.getShell().writeln("bar is set to: "+bar);
            return CommandResult.SUCCESS;
        }
    }
}
