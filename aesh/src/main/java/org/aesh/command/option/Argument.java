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

import org.aesh.command.activator.OptionActivator;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.converter.Converter;
import org.aesh.command.impl.activator.NullActivator;
import org.aesh.command.impl.completer.NullOptionCompleter;
import org.aesh.command.impl.converter.NullConverter;
import org.aesh.command.impl.parser.AeshOptionParser;
import org.aesh.command.impl.renderer.NullOptionRenderer;
import org.aesh.command.impl.validator.NullValidator;
import org.aesh.command.parser.OptionParser;
import org.aesh.command.renderer.OptionRenderer;
import org.aesh.command.validator.OptionValidator;
import org.aesh.selector.SelectorType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A single argument value
 * Is designed to only hold one value, if multiple values are needed use @Arguments
 *
 * Both "=" and space is valid name-value separators.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Argument {

    /**
     * A description of the param.
     * This text will be printed out as part of a usage info.
     */
    String description() default "";

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
     * When this is set to true and used it will override missing Options that are
     * required by the Command.
     */
    boolean overrideRequired() default false;

    /**
     * When set to true æsh will automatically prompt the user to type in a value
     * before the command executes.
     * If a default value has been set, it will override this setting.
     */
    boolean askIfNotSet() default false;

    SelectorType selector() default SelectorType.NO_OP;

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

    /**
     * When true, this argument value is available to all subcommands.
     * Subcommands can access the value via CommandInvocation.getInheritedValue()
     * or it will be auto-populated into a field with the same name.
     * Only valid on group commands.
     */
    boolean inherited() default false;

}
