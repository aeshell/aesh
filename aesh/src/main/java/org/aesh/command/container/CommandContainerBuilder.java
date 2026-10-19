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
package org.aesh.command.container;

import java.lang.reflect.Method;

import org.aesh.command.Command;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;

/**
 * @author Aesh team
 * @author Aesh team
 */
public interface CommandContainerBuilder<CI extends CommandInvocation> {

    CommandContainer<CI> create(Command command) throws CommandLineParserException;

    CommandContainer<CI> create(Class<? extends Command> command) throws CommandLineParserException;

    /**
     * Build a container for a {@code @MethodCommand}-annotated method, to be
     * invoked on the caller-supplied instance. Only public, protected, and
     * package-private methods are supported; private methods are rejected
     * because generated adapters call them directly. For static methods the
     * instance may be null and is ignored.
     *
     * @param instance the object the method is invoked on, or null for static methods
     * @param method the {@code @MethodCommand}-annotated method
     * @return a container whose command executes the method
     * @throws CommandLineParserException if the method cannot serve as a command
     */
    default CommandContainer<CI> create(Object instance, Method method) throws CommandLineParserException {
        throw new UnsupportedOperationException(
                "Method commands are not supported by this container builder");
    }

    /**
     * Build a container for a {@code @MethodCommand} command identified by
     * its command name or alias, to be invoked on the caller-supplied owner
     * object. Generated metadata is preferred when available; otherwise the
     * owner's methods are scanned reflectively as a fallback.
     *
     * @param owner the object the method is invoked on
     * @param commandName the command name or alias
     * @return a container whose command executes the method
     * @throws CommandLineParserException if no matching method serves as a command
     */
    default CommandContainer<CI> create(Object owner, String commandName) throws CommandLineParserException {
        throw new UnsupportedOperationException(
                "Method commands are not supported by this container builder");
    }

    /**
     * Build a container for a static {@code @MethodCommand} command
     * identified by its command name or alias. Only static methods are
     * considered; no owner instance is required.
     *
     * @param ownerClass the class declaring the static method
     * @param commandName the command name or alias
     * @return a container whose command executes the method
     * @throws CommandLineParserException if no matching static method serves as a command
     */
    default CommandContainer<CI> create(Class<?> ownerClass, String commandName)
            throws CommandLineParserException {
        throw new UnsupportedOperationException(
                "Method commands are not supported by this container builder");
    }
}
