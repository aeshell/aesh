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
package org.jboss.aesh.readline;

import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.readline.completion.Completion;
import org.jboss.aesh.readline.editing.EditMode;
import org.jboss.aesh.readline.editing.EditModeBuilder;
import org.jboss.aesh.terminal.formatting.Color;
import org.jboss.aesh.terminal.formatting.TerminalColor;
import org.jboss.aesh.terminal.formatting.TerminalString;
import org.jboss.aesh.tty.Connection;
import org.jboss.aesh.tty.Signal;
import org.jboss.aesh.tty.terminal.TerminalConnection;
import org.jboss.aesh.util.LoggerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href=mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ReadlineConsole {

    private Settings settings;
    private Prompt prompt;
    private List<Completion> completions;
    private TerminalConnection connection;
    private static final Pattern splitter = Pattern.compile("\\w+");
    private static final Logger LOGGER = LoggerUtil.getLogger(ReadlineConsole.class.getName());

    public ReadlineConsole(Settings settings) {
        this.settings = settings;
    }

    public void start() {
        connection = new TerminalConnection();
        init(connection);
    }

    public void init(final Connection conn) {
        Readline readline = new Readline(EditModeBuilder.builder(EditMode.Mode.VI).create());
        read(conn, readline);
    }

    /**
     * Use {@link Readline} to startBlockingReader a user input and then process it
     *
     * @param conn the tty connection
     * @param readline the readline object
     */
    public void read(final Connection conn, final Readline readline) {
        // Just call readline and get a callback when line is startBlockingReader
        readline.readline(conn, prompt, line -> {

            // Ctrl-D
            if (line == null) {
                //((TerminalConnection) conn).stop();
                conn.write("logout\n").close();
                return;
            }

            LOGGER.info("got: " + line);

            //change this and have an api work similar to:
            // Command c = CommandResolver.resolveCommand(line);
            // if(c != null) { new Process(..., c);
            Matcher matcher = splitter.matcher(line);
            if (matcher.find()) {
                String cmd = matcher.group();

                if(cmd.equals("exit")) {
                    conn.write("exiting...\n").close();
                    return;
                }

                // Gather args
                List<String> args = new ArrayList<>();
                while (matcher.find()) {
                    args.add(matcher.group());
                }

                try {
                    //new Process(conn, readline, Command.valueOf(cmd), args).start();
                    return;
                } catch (IllegalArgumentException e) {
                    conn.write(line + ": command not found\n");
                }
            }
            read(conn, readline);
        }, completions);
    }

    public Prompt getPrompt() {
        return prompt;
    }

    public void setPrompt(Prompt prompt) {
        this.prompt = prompt;
    }

    public void addCompletion(Completion completion) {
        if(completions == null)
            completions = new ArrayList<>();
        completions.add(completion);
    }

    public void addCompletions(List<Completion> completions) {
        if(this.completions == null)
            this.completions = new ArrayList<>();
        this.completions.addAll(completions);
    }

    class Process extends Thread implements Consumer<Signal> {

        final Connection conn;
        final Readline readline;
        final Command command;
        volatile boolean running;

        public Process(Connection conn, Readline readline, Command command) {
            this.conn = conn;
            this.readline = readline;
            this.command = command;
        }

        @Override
        public void accept(Signal signal) {
            switch (signal) {
                case INT:
                    if (running) {
                        // Ctrl-C interrupt : we use Thread interrupts to signal the command to stop
                        LOGGER.info("got interrupted in Task");
                        interrupt();
                    }
            }
        }

        @Override
        public void run() {
            // Subscribe to events, in particular Ctrl-C
            conn.setSignalHandler(this);
            running = true;
            try {
                //command.execute(conn, args);
                command.execute(null);
            }
            catch (InterruptedException e) {
                // Ctlr-C interrupt
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                running = false;
                conn.setSignalHandler(null);

                LOGGER.info("trying to read again.");
                // Readline again
                read(conn, readline);
            }
        }
    }

}
