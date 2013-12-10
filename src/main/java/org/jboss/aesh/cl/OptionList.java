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
 * A command line option list
 * Must be defined with a field that implements Collection
 *
 * Eg: --foo=bar1,bar2,bar3 or --foo bar1,bar2,bar3
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface OptionList {

    /**
     * The name of the option param.
     * The first letter will be used as the short name if shortName is not defined
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
     * If set will force the option to be of the form:
     * name[separator]value
     * As an example, if we want to create an option like: --foo bar1,bar2
     */
    char valueSeparator() default ',';

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
