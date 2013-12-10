/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command.converter;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshConverterInvocation implements ConverterInvocation {

    private String input;

    public AeshConverterInvocation(String input) {
        this.input = input;
    }

    @Override
    public String getInput() {
        return input;
    }
}
