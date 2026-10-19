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
package org.aesh.command.impl.container;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.internal.FieldAccessor;
import org.aesh.command.invocation.CommandInvocation;

/**
 * Synthetic {@link Command} backing a {@code @MethodCommand}-annotated
 * method. Converted option values are written into the slot array during
 * population (through per-parameter {@link FieldAccessor}s, reusing the
 * full converter/validator/default machinery); at execution time the
 * slots become the reflective call's arguments.
 * <p>
 * The holding instance is caller-supplied and shared across executions,
 * exactly like instance-registered field commands. Only the method's own
 * parameters are stored here; all parse state lives in the options.
 */
class MethodCommandAdapter<CI extends CommandInvocation> implements Command<CI> {

    private final Object instance;
    private final Method method;
    private final Object[] slots;
    private final boolean[] invocationParams;
    private final Class<?>[] paramTypes;

    MethodCommandAdapter(Object instance, Method method, Object[] slots) {
        this.instance = instance;
        this.method = method;
        this.slots = slots;
        Parameter[] parameters = method.getParameters();
        this.invocationParams = new boolean[parameters.length];
        this.paramTypes = new Class<?>[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            paramTypes[i] = parameters[i].getType();
            invocationParams[i] = CommandInvocation.class.isAssignableFrom(paramTypes[i]);
        }
    }

    /**
     * Accessor writing converted values into the slot for one parameter.
     * The instance argument is ignored: slots belong to the execution
     * registration, not to any command object.
     */
    static FieldAccessor slotAccessor(Object[] slots, int index) {
        return new FieldAccessor() {
            @Override
            public void set(Object instance, Object value) {
                slots[index] = value;
            }

            @Override
            public Object get(Object instance) {
                return slots[index];
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
        Object[] args = new Object[slots.length];
        for (int i = 0; i < slots.length; i++) {
            if (invocationParams[i]) {
                args[i] = commandInvocation;
            } else if (slots[i] != null) {
                args[i] = slots[i];
            } else {
                args[i] = primitiveDefault(paramTypes[i]);
            }
        }
        try {
            Object result = method.invoke(instance, args);
            if (result == null)
                return CommandResult.SUCCESS;
            return (CommandResult) result;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CommandException)
                throw (CommandException) cause;
            if (cause instanceof InterruptedException)
                throw (InterruptedException) cause;
            if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            if (cause instanceof Error)
                throw (Error) cause;
            throw new CommandException(cause);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new CommandException("Failed to invoke @MethodCommand method " + method, e);
        }
    }

    private static Object primitiveDefault(Class<?> type) {
        if (type == boolean.class)
            return Boolean.FALSE;
        if (type == char.class)
            return '\u0000';
        if (type == byte.class)
            return (byte) 0;
        if (type == short.class)
            return (short) 0;
        if (type == int.class)
            return 0;
        if (type == long.class)
            return 0L;
        if (type == float.class)
            return 0f;
        if (type == double.class)
            return 0d;
        return null;
    }
}
