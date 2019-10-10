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

package org.aesh.command;

import org.aesh.command.impl.activator.NullCommandActivator;
import org.aesh.command.impl.validator.NullCommandValidator;
import org.aesh.command.impl.result.NullResultHandler;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.validator.CommandValidator;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import org.aesh.command.activator.CommandActivator;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface GroupCommandDefinition {

    /**
     * Specifies the command name
     *
     * @return command name
     */
    String name();

    /**
     * Add any aliases connected to this command
     * Not required
     *
     * @return alias names
     */
    String[] aliases() default {};

    /**
     * Specify a description of the command
     *
     * @return command description
     */
    String description();

    /**
     * Automatically generate a help option to the command
     * Set to false by default
     *
     * @return generate help option
     */
    boolean generateHelp() default false;

    /**
     * Specify a CommandValidator that will be called before the command is executed.
     *
     * @return CommandValidator
     */
    Class<? extends CommandValidator> validator() default NullCommandValidator.class;

    /**
     * Specify a ResultHandler that will be called after the command finished execution.
     *
     * @return ResultHandler
     */
    Class<? extends ResultHandler> resultHandler() default NullResultHandler.class;

    /**
     * Specify sub commands
     *
     * @return sub commands
     */
    Class<? extends Command>[] groupCommands() default {};

    /**
     * Specify a CommandActivator which will verify if a command can be activated/accessed.
     *
     * @return CommandActivator
     */
    Class<? extends CommandActivator> activator() default NullCommandActivator.class;
}
