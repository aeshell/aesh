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

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.OptionList;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.io.Resource;
import org.jboss.aesh.readline.Prompt;
import org.jboss.aesh.readline.ReadlineConsole;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Example {

    private static boolean masking = false;
    private static String password;
    private static String username;
    static boolean hasPassword;

    public static void main(String[] args) throws IOException {

        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ExitCommand.class)
                .command(LsCommand.class)
                .create();
        ReadlineConsole console = new ReadlineConsole(new SettingsBuilder().commandRegistry(registry).create());

        console.setPrompt(new Prompt("[aesh@rules]$ "));

        console.start();

        /*
        //Settings.getInstance().setAnsiConsole(false);
        SettingsBuilder builder = new SettingsBuilder();
        builder.readInputrc(false).logging(true);
        builder.logfile(System.getProperty("user.dir") + System.getProperty("file.separator") + "aesh_example.log");
        //Settings.getInstance().setAliasEnabled(true);
        //Settings.getInstance().setAliasFile(new File(System.getProperty("user.dir")+Config.getPathSeparator()+"aesh_aliases.txt"));
        //Settings.getInstance().setPersistAlias(true);
        //Settings.getInstance().enableOperatorParser(false);
        //Settings.getInstance().setHistoryDisabled(true);
        //Settings.getInstance().setHistoryPersistent(false);
        List<TerminalCharacter> chars = new ArrayList<TerminalCharacter>();
        chars.add(new TerminalCharacter('[', new TerminalColor(Color.BLUE, Color.DEFAULT)));
        chars.add(new TerminalCharacter('t', new TerminalColor(Color.RED, Color.DEFAULT),
                CharacterType.ITALIC));
        chars.add(new TerminalCharacter('e', new TerminalColor(Color.RED, Color.DEFAULT),
                CharacterType.INVERT));
        chars.add(new TerminalCharacter('s', new TerminalColor(Color.RED, Color.DEFAULT),
                CharacterType.CROSSED_OUT));
        chars.add(new TerminalCharacter('t', new TerminalColor(Color.RED ,Color.DEFAULT),
                CharacterType.BOLD));
        chars.add(new TerminalCharacter(']', new TerminalColor(Color.BLUE, Color.DEFAULT),
                CharacterType.FAINT));
        chars.add(new TerminalCharacter('$', new TerminalColor(Color.GREEN, Color.DEFAULT),
                CharacterType.UNDERLINE));
        chars.add(new TerminalCharacter(' ', new TerminalColor(Color.DEFAULT, Color.DEFAULT)));

        final Prompt prompt = new Prompt(chars);
        //String prompt = ANSI.redText()+"[test@foo]"+ANSI.reset()+"$ ";

        //a simple interruptHook
        builder.interruptHook((console, action) -> {
            if(action.name().equals("interrupt")) {
                console.getShell().out().println("^C");
                console.clearBufferAndDisplayPrompt();
            }
            else if(action.name().equals("ignore-eof")) {
                console.getShell().out().println("Use \"exit\" to leave the shell.");
                console.clearBufferAndDisplayPrompt();
            }
            else {
                console.getShell().out().println();
                console.stop();
            }
        });

        final Console exampleConsole = new Console(builder.create());

        Completion completer = co -> {
            // very simple completor
            List<String> commands = new ArrayList<String>();
            if(co.getBuffer().equals("fo") || co.getBuffer().equals("foo")) {
                commands.add("foo");
                commands.add("foobaa");
                commands.add("foobar");
                commands.add("foobaxxxxxx");
                commands.add("foobbx");
                commands.add("foobcx");
                commands.add("foobdx");
            }
            if(co.getBuffer().equals("p")) {
                commands.add("profile=foo");
                co.setOffset(0);
            }
           if(co.getBuffer().equals("profile="))
                commands.add("profile=foo");
            if(co.getBuffer().equals("profile="))
                commands.add("profile=bar");
            if(co.getBuffer().equals("--")) {
                commands.add("--help-");
            }
            if("--help-me".startsWith(co.getBuffer())) {
                commands.add("--help-me");
            }
            if(co.getBuffer().equals("fooba")) {
                commands.add("foobaa");
                commands.add("foobar");
                commands.add("foobaxxxxxx");
            }
            if(co.getBuffer().equals("foobar")) {
                commands.add("foobar");
            }
            if(co.getBuffer().equals("bar")) {
                commands.add("bar/");
            }
            if(co.getBuffer().equals("h")) {
                commands.add("help.history");
                commands.add("help");
                co.setOffset(0);
            }
            if(co.getBuffer().equals("help")) {
                commands.add("help.history");
                commands.add("help");
            }
            if(co.getBuffer().equals("help.")) {
                commands.add("help.history");
            }
            if(co.getBuffer().equals("deploy")) {
                commands.add("deploy /home/blabla/foo/bar/alkdfe/en/to/tre");
            }
            if(co.getBuffer().equals("testing")) {
                commands.add("testing YAY");
            }
            if(co.getBuffer().equals("val") ||
                    co.getBuffer().equals("value ")) {
                commands.add("value 1");
                commands.add("value 2");
                commands.add("value 10");
                commands.add("value 20");
            }
            if(co.getBuffer().equals("valu"))
                commands.add("value 10");

            co.setCompletionCandidates(commands);
        };

        exampleConsole.addCompletion(completer);

        final ConsoleCallback consoleCallback = new AeshConsoleCallback() {
            @Override
            public int execute(ConsoleOperation output) throws InterruptedException {
                try {
                //To change body of implemented methods use File | Settings | File Templates.
                exampleConsole.getShell().out().println("======>\"" + output.getBuffer());
                if(masking) {
                    exampleConsole.getShell().out().print("got password: " + output.getBuffer() + ", stopping masking");
                    masking = false;
                    exampleConsole.setPrompt(prompt);
                }
                else if (output.getBuffer().equalsIgnoreCase("quit") || output.getBuffer().equalsIgnoreCase("exit") ||
                        output.getBuffer().equalsIgnoreCase("reset")) {
                    exampleConsole.stop();
                }
                else if(output.getBuffer().equalsIgnoreCase("password")) {
                    masking = true;
                    exampleConsole.setPrompt(new Prompt("password: ", (char) 0));
                }
                else if(output.getBuffer().startsWith("blah")) {
                    exampleConsole.getShell().err().println("blah. command not found.");
                    exampleConsole.getShell().out().print("BAH" + Config.getLineSeparator());
                }
                else if(output.getBuffer().equals("clear"))
                    exampleConsole.clear();
                else if(output.getBuffer().startsWith("man")) {
                    //exampleConsole.attachProcess(test);
                    //man = new ExampleConsoleCommand(exampleConsole, output);
                    exampleConsole.getShell().out().println("trying to wait for input");
                    exampleConsole.getShell().out().println("got: " + exampleConsole.getInputLine());
                    //exampleConsole.attachProcess(test);
                }
                else if(output.getBuffer().startsWith("login")) {
                    exampleConsole.setConsoleCallback(passwordCallback);
                    exampleConsole.setPrompt(new Prompt("Username: "));
                }
                 return 0;
                }
                catch (IOException ioe) {
                    exampleConsole.getShell().out().println("Exception: "+ioe.getMessage());
                    return -1;
                }
            }
        };

        exampleConsole.setConsoleCallback(consoleCallback);
        exampleConsole.start();
        exampleConsole.setPrompt(prompt);

        passwordCallback = new AeshConsoleCallback() {
            private boolean hasUsername = false;

            @Override
            public int execute(ConsoleOperation output) throws InterruptedException {
                if(hasUsername) {
                    password = output.getBuffer();
                    hasPassword = true;
                    exampleConsole.getShell().out().print("Username: " + username + ", password: " + password + Config.getLineSeparator());
                    exampleConsole.setPrompt(prompt);
                    exampleConsole.setConsoleCallback(consoleCallback);
                }
                else {
                    username = output.getBuffer();
                    exampleConsole.setPrompt( new Prompt("Password: ", (char) 0));
                    hasUsername = true;
                }
                return 0;
            }
        };

        //show how we can change the prompt async
        try {
            Thread.sleep(4000);
            exampleConsole.setPrompt(new Prompt(
                    new TerminalString("[FOO]» ", new TerminalColor( Color.RED, Color.DEFAULT), new TerminalTextStyle(CharacterType.BOLD))));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static class ExampleConsoleCommand {

        private final Console console;
        private final ConsoleOperation operation;

        public ExampleConsoleCommand(Console console, ConsoleOperation operation) {
            this.console = console;
            this.operation = operation;

            init();
        }

        private void init() {
            try {
                if(!operation.getControlOperator().isRedirectionOut()) {
                    console.getShell().out().print(ANSI.ALTERNATE_BUFFER);
                    console.getShell().out().println("print alternate screen...");
                    console.getShell().out().flush();
                }

                if(console.getShell().in().getStdIn().available() > 0) {
                    java.util.Scanner s = new java.util.Scanner(console.getShell().in().getStdIn()).useDelimiter("\\A");
                    String fileContent = s.hasNext() ? s.next() : "";
                    console.getShell().out().println("FILECONTENT: ");
                    console.getShell().out().print(fileContent);
                    console.getShell().out().flush();
                }
                else
                    console.getShell().out().println("console.in() == null");


                readFromFile();

                //detach after init if hasRedirectOut()
                if(operation.getControlOperator().isRedirectionOut()) {
                }

                console.getShell().out().println("trying to wait on input");
                int input = console.getShell().in().getStdIn().read();
                console.getShell().out().println("we got: "+input);
            }
            catch(IOException ioe) {

            }
        }

        private void readFromFile() throws IOException {
            if(console.getShell().in().getStdIn().available() > 0) {
                console.getShell().out().println("FROM STDOUT: ");
            }
            else
                console.getShell().out().println("here should we present some text... press 'q' to quit");
        }

        public void processOperation(CommandOperation operation) throws IOException {
            if(operation.getInput()[0] == 'q') {
                console.getShell().out().print(ANSI.MAIN_BUFFER);
            }
            else if(operation.getInput()[0] == 'a') {
                readFromFile();
            }
            else {

            }
        }

*/
    }

    @CommandDefinition(name = "exit", description = "exit the program", aliases = {"quit"})
    public static class ExitCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.stop();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name="ls", description = "[OPTION]... [FILE]...")
    public static class LsCommand implements Command {

        @Option(shortName = 'f', hasValue = false, description = "set foo to true/false")
        private Boolean foo;

        @Option(hasValue = false, description = "set the bar") //, renderer = BlueBoldRenderer.class)
        private boolean bar;

        @Option(shortName = 'l', completer = AeshExample.LessCompleter.class, defaultValue = {"MORE"}, argument = "SIZE")
        private String less;

        @OptionList(defaultValue = "/tmp", description = "file location", valueSeparator = ':') //,
                //validator = DirectoryValidator.class,
                //activator = BarActivator.class)
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

}
