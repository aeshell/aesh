/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.settings.Settings;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConsoleBuilder {

    private Set<Class<? extends Command>> commands;
    private Settings settings;
    private Prompt prompt;

    public AeshConsoleBuilder() {
        commands = new HashSet<Class<? extends Command>>();
    }

    public AeshConsoleBuilder command(Class<? extends Command> command) {
        commands.add(command);
        return this;
    }

    public AeshConsoleBuilder settings(Settings settings) {
        this.settings = settings;
        return this;
    }

    public AeshConsoleBuilder prompt(Prompt prompt) {
        this.prompt = prompt;
        return this;
    }


    public AeshConsole create() {
        AeshConsole aeshConsole = new AeshConsoleImp(settings);
        for(Class<? extends Command> command : commands)
            aeshConsole.addCommand(command);

        aeshConsole.setPrompt(prompt);

        return aeshConsole;
    }


}
