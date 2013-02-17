/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

import org.jboss.aesh.console.ConsoleCommand;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOutput;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.helper.InterruptHook;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.CharacterType;
import org.jboss.aesh.terminal.Color;
import org.jboss.aesh.terminal.TerminalCharacter;
import org.jboss.aesh.util.ANSI;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Example {

    public static void main(String[] args) throws IOException {

        //Settings.getInstance().setAnsiConsole(false);
        Settings.getInstance().setReadInputrc(true);
        Settings.getInstance().setLogging(true);
        Settings.getInstance().setLogFile("aesh_example.log");
       //Settings.getInstance().setHistoryDisabled(true);
        //Settings.getInstance().setHistoryPersistent(false);
        List<TerminalCharacter> chars = new ArrayList<TerminalCharacter>();
        chars.add(new TerminalCharacter('[', Color.DEFAULT_BG, Color.BLUE_TEXT));
        chars.add(new TerminalCharacter('t', Color.DEFAULT_BG, Color.RED_TEXT,
                CharacterType.ITALIC));
        chars.add(new TerminalCharacter('e', Color.DEFAULT_BG, Color.RED_TEXT,
                CharacterType.INVERT));
        chars.add(new TerminalCharacter('s', Color.DEFAULT_BG, Color.RED_TEXT,
                CharacterType.CROSSED_OUT));
        chars.add(new TerminalCharacter('t', Color.DEFAULT_BG ,Color.RED_TEXT,
                CharacterType.BOLD));
        chars.add(new TerminalCharacter(']', Color.DEFAULT_BG, Color.BLUE_TEXT,
                CharacterType.PLAIN));
        chars.add(new TerminalCharacter('$', Color.DEFAULT_BG, Color.WHITE_TEXT,
                CharacterType.UNDERLINE));
        chars.add(new TerminalCharacter(' ', Color.DEFAULT_BG, Color.WHITE_TEXT));

        Prompt prompt = new Prompt(chars);
        //String prompt = ANSI.redText()+"[test@foo]"+ANSI.reset()+"$ ";

        //a simple interruptHook
        Settings.getInstance().setInterruptHook(new InterruptHook() {
            @Override
            public void handleInterrupt(Console console) {
                try {
                    console.pushToStdOut("KILLED!\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Console exampleConsole = new Console();

        ConsoleCommand test = new ConsoleCommand(exampleConsole) {

            @Override
            protected void afterAttach() throws IOException {
                if(!hasRedirectOut()) {
                    console.pushToStdOut(ANSI.getAlternateBufferScreen());
                }

                readFromFile();

                //detach after init if hasRedirectOut()
                if(hasRedirectOut()) {
                    detach();
                }
            }

            @Override
            protected void afterDetach() throws IOException {
                if(!hasRedirectOut())
                    console.pushToStdOut(ANSI.getMainBufferScreen());
            }

            private void readFromFile() throws IOException {
                if(getConsoleOutput().getStdOut() != null &&
                        getConsoleOutput().getStdOut().length() > 0) {
                    console.pushToStdOut("FROM STDOUT: "+getConsoleOutput().getStdOut());
                }
                else
                    console.pushToStdOut("here should we present some text... press 'q' to quit");
            }

            @Override
            public void processOperation(Operation operation) throws IOException {
                if(operation.getInput()[0] == 'q') {
                    detach();
                }
                else if(operation.getInput()[0] == 'a') {
                    readFromFile();
                }
                else {

                }
            }
        };

        Completion completer = new Completion() {
            @Override
            public void complete(CompleteOperation co) {
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
                /*
                if(co.getBuffer().equals("p")) {
                    commands.add("profile=bar");
                    co.setOffset(0);
                }
                */
                if(co.getBuffer().equals("profile="))
                    commands.add("profile=foo");
                if(co.getBuffer().equals("profile="))
                    commands.add("profile=bar");
                if(co.getBuffer().equals("--")) {
                    commands.add("--help-");
                }
                //if(co.getBuffer().startsWith("--help-") || co.getBuffer().startsWith("--help-m")) {
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
                co.setCompletionCandidates(commands);
            }
        };

        exampleConsole.addCompletion(completer);

        ConsoleOutput line;
        exampleConsole.pushToStdOut(ANSI.greenBackground());
        //while ((line = exampleConsole.read(prompt)) != null) {
        //while ((line = exampleConsole.read("[test@foo.bar]~> ")) != null) {
        while ((line = exampleConsole.read(prompt)) != null) {
            exampleConsole.pushToStdOut("======>\"" + line.getBuffer() + "\"\n");

            if (line.getBuffer().equalsIgnoreCase("quit") || line.getBuffer().equalsIgnoreCase("exit") ||
                    line.getBuffer().equalsIgnoreCase("reset")) {
                break;
            }
            if(line.getBuffer().equalsIgnoreCase("password")) {
                line = exampleConsole.read(new Prompt("password: ", (char) 0));
                exampleConsole.pushToStdOut("password typed:" + line + "\n");

            }
            //test stdErr
            if(line.getBuffer().startsWith("blah")) {
                exampleConsole.pushToStdErr("blah. command not found.\n");
            }
            if(line.getBuffer().equals("clear"))
                exampleConsole.clear();
            if(line.getBuffer().startsWith("man")) {
                //exampleConsole.attachProcess(test);
                test.attach(line);
            }
        }
        if(line != null && line.getBuffer().equals("reset")) {
            exampleConsole.stop();
            exampleConsole = new Console();

            while ((line = exampleConsole.read("> ")) != null) {
                exampleConsole.pushToStdOut("======>\"" + line + "\"\n");
                if (line.getBuffer().equalsIgnoreCase("quit") || line.getBuffer().equalsIgnoreCase("exit") ||
                        line.getBuffer().equalsIgnoreCase("reset")) {
                    break;
                }

            }
        }

        try {
            exampleConsole.stop();
        } catch (Exception e) {
        }
    }

}
