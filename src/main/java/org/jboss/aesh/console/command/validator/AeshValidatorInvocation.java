/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command.validator;

import org.jboss.aesh.console.AeshContext;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshValidatorInvocation<C> implements ValidatorInvocation<Object, C> {

    private final Object value;
    private final C command;
    private final AeshContext aeshContext;

    public AeshValidatorInvocation(Object value, C command, AeshContext aeshContext) {
        this.value = value;
        this.command = command;
        this.aeshContext = aeshContext;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public C getCommand() {
        return command;
    }

    @Override
    public AeshContext getAeshContext() {
        return aeshContext;
    }
}
