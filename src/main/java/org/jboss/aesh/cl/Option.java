/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.activation.NullActivator;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.completer.NullOptionCompleter;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.cl.converter.Converter;
import org.jboss.aesh.cl.converter.NullConverter;
import org.jboss.aesh.cl.renderer.NullOptionRenderer;
import org.jboss.aesh.cl.renderer.OptionRenderer;
import org.jboss.aesh.cl.validator.NullValidator;
import org.jboss.aesh.cl.validator.OptionValidator;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A single command line option.
 *
 * It keep information regarding the short option name (-f)
 * and long option name (--foo).
 * A flag to specify if this option is required and a description.
 *
 * Both "=" and space is valid name-value separators.
 *
 * If the field is either Boolean or boolean it will be
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Option {

    /**
     * The name of the option param.
     * The first letter will be used as the short name.
     * If name is not defined, the variable name will be used.
     */
    String name() default "";

    /**
     * The short option name.
     */
    char shortName() default '\u0000';

    /**
     * A description of the param.
     * This text will be printed out as part of a usage info.
     */
    String description() default "";

    /**
     * A description on what kind of value is used for this option.
     */
    String argument() default "";

    /**
     * Specify if this option is required
     */
    boolean required() default false;

    /**
     * If this options has a value the default will be auto completed if it matches
     * the value already typed
     */
    String[] defaultValue() default {};

    /**
     * Specify if this option accept values.
     * Only accepted to be false for Boolean/boolean
     */
    boolean hasValue() default true;

    /**
     * When this is set to true and used it will override missing Options that are
     * required by the Command.
     */
    boolean overrideRequired() default false;

    /**
     * Define a converter if the field is a type thats not java.lang and other
     * common types, eg: File,++
     * See ClConverterManager for whats added by default
     */
    Class<? extends Converter> converter() default NullConverter.class;

    /**
     * Define a completer for this option.
     */
    Class<? extends OptionCompleter> completer() default NullOptionCompleter.class;

    /**
     * Define a validator for this option.
     */
    Class<? extends OptionValidator> validator() default NullValidator.class;

    /**
     * Define an activator for this option.
     */
    Class<? extends OptionActivator> activator() default NullActivator.class;

    /**
     * Define a renderer
     */
    Class<? extends OptionRenderer> renderer() default NullOptionRenderer.class;
}
