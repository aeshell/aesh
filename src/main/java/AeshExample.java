/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Command;
import org.jboss.aesh.console.InjectConsole;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;

import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshExample {

    public static void main(String[] args) {

        Settings settings = new SettingsBuilder().logging(true).create();
        AeshConsole aeshConsole = new AeshConsoleBuilder().settings(settings)
                .prompt(new Prompt("[aesh@rules]$ "))
                .command(ExitCommand.class)
                .command(FooCommand.class)
                .create();

        aeshConsole.start();

    }


    @CommandDefinition(name="exit", description = "exit the program")
    public static class ExitCommand implements Command {

        @InjectConsole
        private AeshConsole console;

        @Override
        public void execute() throws IOException {
            console.stop();
        }
    }

    @CommandDefinition(name="foo", description = "fooing")
    public static class FooCommand implements Command {

        @Option
        private String bar;

        @InjectConsole
        private AeshConsole console;

        @Override
        public void execute() throws IOException {
           if(bar == null)
               console.printToStdOut("NO BAR!");
            else
               console.printToStdOut("you set bar to: "+bar);
        }
    }

}
