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
package org.aesh.command.invocation;

import org.aesh.command.impl.invocation.AeshCommandInvocationProvider;

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
