/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.cl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Parameter represent a list of {@link Option} objects,
 * a usage description and which parser to use for a command.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Parameter {

    /**
     * The command name
     */
    String name() default "";

    /**
     * General usage information that will be used when usage/help info
     * is displayed.
     */
    String usage() default "";

    /**
     * All the defined options for this command
     */
    Option[] options() default {};
}
