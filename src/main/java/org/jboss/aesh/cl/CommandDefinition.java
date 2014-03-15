/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.result.NullResultHandler;
import org.jboss.aesh.cl.result.ResultHandler;
import org.jboss.aesh.cl.validator.CommandValidator;
import org.jboss.aesh.cl.validator.NullCommandValidator;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface CommandDefinition {

    String name();

    String description();

    Class<? extends CommandValidator> validator() default NullCommandValidator.class;

    Class<? extends ResultHandler> resultHandler() default NullResultHandler.class;
}
