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

import org.aesh.command.parser.CommandLineParserException;

/**
 * The definition of a registered command is invalid, for example an
 * {@code @OptionList} field that is not a Collection. With deferred
 * registration (#678) this surfaces on first use instead of at registration.
 * <p>
 * It extends {@link CommandNotFoundException} so existing callers keep
 * working, but the command was found: report the cause, not "not found".
 *
 * @author Aesh team
 * @since 3.18.7
 */
public class CommandDefinitionException extends CommandNotFoundException {

    public CommandDefinitionException(String cmdName, CommandLineParserException cause) {
        super("Invalid definition of command " + cmdName + ": " + cause.getMessage(), cmdName);
        initCause(cause);
    }
}
