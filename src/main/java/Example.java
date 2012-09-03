/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

import org.jboss.jreadline.console.ConsoleCommand;
import org.jboss.jreadline.complete.CompleteOperation;
import org.jboss.jreadline.complete.Completion;
import org.jboss.jreadline.console.Console;
import org.jboss.jreadline.console.ConsoleOutput;
import org.jboss.jreadline.console.settings.Settings;
import org.jboss.jreadline.edit.actions.Operation;
import org.jboss.jreadline.util.ANSI;

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
        Settings.getInstance().setReadInputrc(false);
       //Settings.getInstance().setHistoryDisabled(true);
        //Settings.getInstance().setHistoryPersistent(false);
        Console exampleConsole = new Console();

        PrintWriter out = new PrintWriter(System.out);

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
                //console.pushToStdOut("blablablablablabal");
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
                if(co.getBuffer().equals("--")) {
                    commands.add("--help-");
                }
                if(co.getBuffer().startsWith("--help-") || co.getBuffer().startsWith("--help-m")) {
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
        //console.pushToStdOut(ANSI.GREEN_TEXT());
        while ((line = exampleConsole.read("[test@foo.bar]~> ")) != null) {
            exampleConsole.pushToStdOut("======>\"" + line.getBuffer() + "\"\n");

            if (line.getBuffer().equalsIgnoreCase("quit") || line.getBuffer().equalsIgnoreCase("exit") ||
                    line.getBuffer().equalsIgnoreCase("reset")) {
                break;
            }
            if(line.getBuffer().equalsIgnoreCase("password")) {
                line = exampleConsole.read("password: ", Character.valueOf((char) 0));
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
