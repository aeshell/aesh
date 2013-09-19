/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.command.CommandRegistry;
import org.jboss.aesh.console.command.MutableCommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConsoleBuilder {

    private Settings settings;
    private Prompt prompt;
    private CommandRegistry registry;

    public AeshConsoleBuilder() {
    }

    public AeshConsoleBuilder commandRegistry(CommandRegistry registry) {
        this.registry = registry;
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
        if(settings == null)
            settings = new SettingsBuilder().create();
        if(registry == null) {
            registry = new MutableCommandRegistry();
        }
        AeshConsole aeshConsole = new AeshConsoleImpl(settings, registry);

        if(prompt != null)
            aeshConsole.setPrompt(prompt);

        return aeshConsole;
    }


}
