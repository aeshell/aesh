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
package org.aesh.cl;

import org.aesh.cl.activation.NullCommandActivator;
import org.aesh.cl.validator.NullCommandValidator;
import org.aesh.cl.result.NullResultHandler;
import org.aesh.cl.result.ResultHandler;
import org.aesh.cl.validator.CommandValidator;
import org.aesh.console.command.Command;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import org.aesh.cl.activation.CommandActivator;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface GroupCommandDefinition {

    String name();

    String[] aliases() default {};

    String description();

    Class<? extends CommandValidator> validator() default NullCommandValidator.class;

    Class<? extends ResultHandler> resultHandler() default NullResultHandler.class;

    Class<? extends Command>[] groupCommands() default {};

    Class<? extends CommandActivator> activator() default NullCommandActivator.class;
}
