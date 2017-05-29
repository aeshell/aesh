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
package org.aesh.readline;

import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandRuntime;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.CommandException;
import org.aesh.terminal.tty.Signal;
import org.aesh.utils.Config;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.invocation.AeshCommandInvocation;
import org.aesh.terminal.Connection;
import org.aesh.util.LoggerUtil;

import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Process extends Thread implements Consumer<Signal> {

    private final Connection conn;
    private final Readline readline;
    private final String line;
    private final Console console;
    private final CommandRuntime<AeshCommandInvocation> runtime;
    private volatile boolean running;

    private static final Logger LOGGER = LoggerUtil.getLogger(Process.class.getName());

    public Process(Connection conn, Console console, Readline readline,
                   CommandRuntime<AeshCommandInvocation> runtime, String line) {
        this.conn = conn;
        this.console = console;
        this.readline = readline;
        this.runtime = runtime;
        this.line = line;
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

        CommandResult result;
        ResultHandler resultHandler = null;
        try {
            //resultHandler = container.getParser().getProcessedCommand().resultHandler();
            //CommandContainerResult ccResult = runCommand(container, line);
            runtime.executeCommand(line);

            //result = ccResult.getCommandResult();

            //if(result == CommandResult.SUCCESS && resultHandler != null)
            //    resultHandler.onSuccess();
            //else if(resultHandler != null)
            //    resultHandler.onFailure(result);
        }
        catch (CommandLineParserException | CommandValidatorException | OptionValidatorException |
                CommandException e) {
            conn.write(e.getMessage()+ Config.getLineSeparator());
        }
        catch (CommandNotFoundException cnfe) {

        }
        catch (InterruptedException e) {
            // Ctlr-C interrupt
        }
        catch (Exception e) {
            e.printStackTrace();
         }
        finally {
            running = false;
            if(conn.getSignalHandler() != null)
                conn.setSignalHandler(null);
            if(conn.getStdinHandler() != null)
                conn.setStdinHandler(null);

            if (console.running()) {
                // Readline again
                console.read(conn, readline);
            }
            else {
                conn.close();
            }
        }
    }
}
