/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console;

import org.jboss.aesh.console.command.activator.OptionActivatorProvider;
import org.jboss.aesh.console.command.completer.CompleterInvocationProvider;
import org.jboss.aesh.console.command.converter.ConverterInvocationProvider;
import org.jboss.aesh.console.command.validator.ValidatorInvocationProvider;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshInvocationProviders implements InvocationProviders {

    private final ConverterInvocationProvider converterInvocationProvider;
    private final CompleterInvocationProvider completerInvocationProvider;
    private final ValidatorInvocationProvider validatorInvocationProvider;
    private final OptionActivatorProvider optionActivatorProvider;

    public AeshInvocationProviders(ConverterInvocationProvider converterInvocationProvider,
                                   CompleterInvocationProvider completerInvocationProvider,
                                   ValidatorInvocationProvider validatorInvocationProvider,
                                   OptionActivatorProvider optionActivatorProvider) {
        this.converterInvocationProvider = converterInvocationProvider;
        this.completerInvocationProvider = completerInvocationProvider;
        this.validatorInvocationProvider = validatorInvocationProvider;
        this.optionActivatorProvider = optionActivatorProvider;
    }

    @Override
    public ConverterInvocationProvider getConverterProvider() {
        return converterInvocationProvider;
    }

    @Override
    public CompleterInvocationProvider getCompleterProvider() {
        return completerInvocationProvider;
    }

    @Override
    public ValidatorInvocationProvider getValidatorProvider() {
        return validatorInvocationProvider;
    }

    @Override
    public OptionActivatorProvider getOptionActivatorProvider() {
        return optionActivatorProvider;
    }
}
