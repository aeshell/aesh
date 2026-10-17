/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Declares a method as a command, analogous to picocli's method commands.
 * The annotated method serves as a subcommand; its parameters carry
 * {@code @Option} or {@code @Argument} annotations and are populated
 * through the same machinery as field-based options. The method is
 * invoked on a caller-supplied instance of the declaring class.
 * <p>
 * Only {@code void} methods or methods returning {@link CommandResult}
 * are supported. Anything else fails at registration time.
 *
 * @author Aesh team
 */
@Inherited
@Retention(RUNTIME)
@Target(METHOD)
public @interface MethodCommand {

    /**
     * Specifies the command name. When empty, the name is derived verbatim
     * from the annotated method's name, mirroring picocli method commands.
     * Explicit names always win, and must be unique within the declaring
     * class: Java overloads sharing a derived or explicit name collide on
     * the command line and fail registration.
     *
     * @return command name, or empty to derive it from the method name
     */
    String name() default "";

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
}
