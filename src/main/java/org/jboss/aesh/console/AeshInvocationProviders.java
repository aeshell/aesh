/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.command.completer.CompleterInvocationProvider;
import org.jboss.aesh.console.command.converter.ConverterInvocationProvider;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshInvocationProviders implements InvocationProviders {

    private ConverterInvocationProvider converterInvocationProvider;
    private CompleterInvocationProvider completerInvocationProvider;

    public AeshInvocationProviders(ConverterInvocationProvider converterInvocationProvider,
                                   CompleterInvocationProvider completerInvocationProvider) {
        this.converterInvocationProvider = converterInvocationProvider;
        this.completerInvocationProvider = completerInvocationProvider;
    }

    @Override
    public ConverterInvocationProvider getConverterInvocationProvider() {
        return converterInvocationProvider;
    }

    @Override
    public CompleterInvocationProvider getCompleterInvocationProvider() {
        return completerInvocationProvider;
    }
}
