/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command.invocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandInvocationServices {

    private final Map<String, CommandInvocationProvider> providers =
            new ConcurrentHashMap<String, CommandInvocationProvider>();
    public static final String DEFAULT_PROVIDER_NAME = "default";

    public void registerProvider(String name, CommandInvocationProvider provider) {
        providers.put(name, provider);
    }

    public void registerDefaultProvider(CommandInvocationProvider p) {
        registerProvider(DEFAULT_PROVIDER_NAME, p);
    }

    public CommandInvocationProvider getDefaultCommandInvocationProvider() {
        if(!providers.containsKey(DEFAULT_PROVIDER_NAME))
            providers.put(DEFAULT_PROVIDER_NAME, new AeshCommandInvocationProvider());

        return getCommandInvocationProvider(DEFAULT_PROVIDER_NAME);
    }

    public CommandInvocationProvider getCommandInvocationProvider(String name) {
        CommandInvocationProvider provider = providers.get(name);
        if(provider == null)
            return getDefaultCommandInvocationProvider();
        else
            return provider;
    }

}
