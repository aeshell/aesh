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

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.graphics.AeshGraphicsConfiguration;
import org.jboss.aesh.graphics.Graphics;
import org.jboss.aesh.graphics.GraphicsConfiguration;
import org.jboss.aesh.terminal.Color;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.terminal.TerminalColor;

import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshGraphicsExample {

    public static void main(String[] args) {
        SettingsBuilder builder = new SettingsBuilder().logging(true);
        builder.enableMan(true);

        Settings settings = builder.create();
        CommandRegistry registry = new AeshCommandRegistryBuilder()
                .command(ExitCommand.class)
                .command(new GraphicsCommand())
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
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            commandInvocation.stop();
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "gfx", description = "")
    public static class GraphicsCommand implements Command {

        private CommandInvocation invocation;
        private Graphics g;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            invocation = commandInvocation;
            invocation.getShell().enableAlternateBuffer();
            doGfx();

            return CommandResult.SUCCESS;
        }

        public void waitForInput() {
            try {
                while(!invocation.getInput().equals(Key.q)) {

                }
            }
            catch (InterruptedException ignored) { }
            if(g != null)
                g.clearAndShowCursor();
            invocation.getShell().enableMainBuffer();
        }

        private void doGfx() {
            try {
                GraphicsConfiguration gc = new AeshGraphicsConfiguration(invocation.getShell());
                g = gc.getGraphics();

                g.setColor(new TerminalColor(Color.BLUE, Color.DEFAULT));
                g.drawRect(20, 10, 20, 4);
                Thread.sleep(500);
                g.flush();

                g.setColor(new TerminalColor(Color.RED, Color.DEFAULT));
                g.drawRect(50, 5, 10, 8);
                Thread.sleep(500);
                g.flush();

                g.setColor(new TerminalColor(Color.DEFAULT, Color.YELLOW));
                g.fillRect(0, 15, 16, 10);
                Thread.sleep(500);
                g.flush();

                g.setColor(new TerminalColor(Color.CYAN, Color.DEFAULT));
                g.drawString("ÆSH", 100, 25);
                g.drawString("RULES", 100, 26);
                Thread.sleep(500);
                g.flush();

                g.setColor(new TerminalColor(Color.WHITE, Color.WHITE));
                g.drawRect(80, 23, 40, 5);
                Thread.sleep(500);
                g.flush();


                g.setColor(new TerminalColor(Color.BLUE, Color.DEFAULT));
                g.drawCircle(100, 10, 5);
                g.flush();
                Thread.sleep(500);

                g.setColor(new TerminalColor(Color.DEFAULT, Color.DEFAULT));
                g.drawLine(0, 0, 50, 20);
                g.flush();
                Thread.sleep(1500);

                g.setColor(new TerminalColor(Color.DEFAULT, Color.RED));
                int j =0;
                for(int i=0; i<100; i++) {
                    g.clear();
                    g.fillRect(i, 15+j, 20, 8);
                    g.flush();
                    Thread.sleep(50);
                    if(i > 10 && i < 20 || (i > 30 && i < 40))
                        j++;
                    if(i < 10 || (i > 20 && i < 30) || (i > 40 && i < 50))
                        j--;
                }

                waitForInput();
            }
            catch (InterruptedException ignored) { }
        }

    }

}
