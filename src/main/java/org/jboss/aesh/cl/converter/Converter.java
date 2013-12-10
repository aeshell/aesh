/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.converter;

import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.command.converter.ConverterInvocation;

/**
 * A Command line converter
 * All implementations must have a public constructor with no arguments.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface Converter<T,C extends ConverterInvocation> {
    T convert(C converterInvocation) throws OptionValidatorException;
}
