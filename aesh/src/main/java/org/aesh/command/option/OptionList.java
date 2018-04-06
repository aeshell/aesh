/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.aesh.command.option;

import org.aesh.command.impl.activator.NullActivator;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.impl.completer.NullOptionCompleter;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.converter.Converter;
import org.aesh.command.impl.converter.NullConverter;
import org.aesh.command.impl.parser.AeshOptionParser;
import org.aesh.command.impl.renderer.NullOptionRenderer;
import org.aesh.command.parser.OptionParser;
import org.aesh.command.renderer.OptionRenderer;
import org.aesh.command.impl.validator.NullValidator;
import org.aesh.command.validator.OptionValidator;

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
     * As an example, if we want to build an option like: --foo bar1,bar2
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

    /**
     * Define a parser
     * Only change this if you want to specify a custom parser
     */
    Class<? extends OptionParser> parser() default AeshOptionParser.class;
}
