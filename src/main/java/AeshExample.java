/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.OptionList;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.internal.ProcessedCommandBuilder;
import org.jboss.aesh.cl.internal.ProcessedOptionBuilder;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.renderer.OptionRenderer;
import org.jboss.aesh.cl.validator.OptionValidator;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshConsoleBufferBuilder;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.AeshInputProcessorBuilder;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.ConsoleBuffer;
import org.jboss.aesh.console.InputProcessor;
import org.jboss.aesh.console.command.completer.CompleterInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.validator.ValidatorInvocation;
import org.jboss.aesh.console.command.validator.ValidatorInvocationProvider;
import org.jboss.aesh.console.helper.ManProvider;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.io.Resource;
import org.jboss.aesh.terminal.CharacterType;
import org.jboss.aesh.terminal.Color;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.terminal.TerminalColor;
import org.jboss.aesh.terminal.TerminalString;
import org.jboss.aesh.terminal.TerminalTextStyle;
import org.jboss.aesh.util.ANSI;

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
public class AeshExample {

    public static void main(String[] args) throws CommandLineParserException {

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
                .generateCommand();

        SettingsBuilder builder = new SettingsBuilder().logging(true);
        builder.enableMan(true)
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
        Settings settings = builder.create();
        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ExitCommand.class)
                .command(fooCommand, FooCommand.class)
                .command(LsCommand.class)
                .command(TestConsoleCommand.class)
                .command(PromptCommand.class)
                .command(RunCommand.class)
                .command(GroupCommand.class)
                .create();

        AeshConsole aeshConsole = new AeshConsoleBuilder()
                .commandRegistry(registry)
                .manProvider(new ManProviderExample())
                .settings(settings)
                .validatorInvocationProvider(new ExampleValidatorInvocationProvider())
                .prompt(new Prompt(new TerminalString("[aesh@rules]$ ",
                        new TerminalColor(Color.GREEN, Color.DEFAULT, Color.Intensity.BRIGHT))))
                .create();

        aeshConsole.start();
    }

    @CommandDefinition(name="exit", description = "exit the program")
    public static class ExitCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            commandInvocation.stop();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "run", description = "")
    public static class RunCommand implements Command {

        @Arguments
        private List<Resource> arguments;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {

            commandInvocation.putProcessInBackground();

            if(arguments != null && arguments.size() > 0 && arguments.get(0).isLeaf()) {
                List<String> script = readScriptFile(arguments.get(0));

                for (String line : script) {
                    commandInvocation.executeCommand(line + Config.getLineSeparator());
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
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
           if(bar == null)
               commandInvocation.getShell().out().println("NO BAR!");
            else
               commandInvocation.getShell().out().println("you set bar to: " + bar);
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
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            this.shell = commandInvocation.getShell();
            if(help) {
                shell.out().println(commandInvocation.getHelpInfo("test"));
            }
            else {
                //display();
                processOperation(commandInvocation);
            }

            return CommandResult.SUCCESS;
        }

        private void display() {
            shell.out().print(ANSI.getAlternateBufferScreen());
            shell.out().flush();
        }

        private void stop() {
            shell.out().print(ANSI.getMainBufferScreen());
        }

        public void processOperation(CommandInvocation invocation) throws IOException, InterruptedException {
            //first ask for username, then password
            String username = promptForInput("username: ", null, invocation);
            String password = promptForInput("password: ", '*', invocation);

            shell.out().println("we got username: "+username+", password: "+password);
        }

        private String promptForInput(String prompt, Character mask,
                                      CommandInvocation invocation) throws IOException, InterruptedException {

            ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder()
                    .shell(invocation.getShell())
                    .prompt(new Prompt(prompt, mask))
                    .create();
            InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                    .consoleBuffer(consoleBuffer)
                    .create();

            consoleBuffer.displayPrompt();
                String result;
                do {
                    result = inputProcessor.parseOperation(invocation.getInput());
                }
                while(result == null );
                return result;
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

        @Arguments
        private List<Resource> arguments;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
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
                    for(Resource f : arguments)
                        commandInvocation.getShell().out().println(f.toString());
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
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            this.shell = commandInvocation.getShell();
            if(bar) {
                shell.out().print("are you sure you want bar? (y/n) ");
                CommandOperation operation = null;
                try {
                    operation = commandInvocation.getInput();
                }
                catch (InterruptedException e) {
                    return CommandResult.FAILURE;
                }
                processOperation(operation);
            }
            return CommandResult.SUCCESS;
        }

        public void processOperation(CommandOperation operation) throws IOException {
            if(operation.getInputKey() == Key.y) {
                shell.out().println(Config.getLineSeparator()+"you wanted bar!");
            }
            else
                shell.out().println(Config.getLineSeparator()+"you chickened out!!");
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

    @GroupCommandDefinition(name = "group", description = "", groupCommands = {Child1.class, Child2.class})
    public static class GroupCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            commandInvocation.getShell().out().println("only executed group, it doesnt do much...");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child1", description = "")
    public static class Child1 implements Command {
        @Option
        private boolean foo;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            commandInvocation.getShell().out().println("foo is set to: "+foo);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child2", description = "")
    public static class Child2 implements Command {
        @Option
        private boolean bar;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            commandInvocation.getShell().out().println("bar is set to: "+bar);
            return CommandResult.SUCCESS;
        }
    }
}
