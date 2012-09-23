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
 * A single command line option.
 * It keep information regarding the short option name (-f)
 * and long option name (--foo).
 * A flag to specify if this option is required and a description.
 *
 * Both "=" and space is valid name-value separators.
 *
 * This class is part of a {@link Parameter} structure.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Option {

    /**
     * The name of the default option param.
     */
    char name() default '\u0000';

    /**
     * An optional long name.
     * Either name() or longName() must be set
     */
    String longName() default "";

    /**
     * A description of the param.
     * This text will be printed out as part of a usage info.
     */
    String description() default "";

    /**
     * Is this option just a flag or do it contain a value
     * By default it is mapped as a flag
     */
    boolean hasValue() default false;

    /**
     * A description on what kind of value is used for this option.
     */
    String argument() default "";

    /**
     * If set will force the option to be of the form:
     * name[separator]value
     * As an example, if we want to create an option like: --foo bar1,bar2
     * {@code @Option(longName='foo', valueSeparator=',')}
     */
    char valueSeparator() default '\u0000';

    /**
     * Specify if this option is of the type:
     * name=value. Eg:
     * -Dname=value
     * The default separator is '=',
     * but it can be changed with {@code valueSeparator()}
     */
    boolean isProperty() default false;

    /**
     * Specify if this option is required
     */
    boolean required() default false;

}
