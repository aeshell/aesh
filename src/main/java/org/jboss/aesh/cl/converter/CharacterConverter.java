/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.converter;

import org.jboss.aesh.console.command.converter.ConverterInvocation;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CharacterConverter implements Converter<Character, ConverterInvocation> {
    @Override
    public Character convert(ConverterInvocation input) {
        if(input != null && input.getInput().length() > 0)
            return input.getInput().charAt(0);
        else
            return '\u0000';
    }
}
