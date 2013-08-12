/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.converter.CLConverter;

import java.util.Currency;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CurrencyConverter implements CLConverter<Currency> {
    @Override
    public Currency convert(String input) {
        return Currency.getInstance(input);
    }
}
