/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017 Red Hat Inc. and/or its affiliates and other contributors
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
package org.aesh.command.registry;

import java.util.List;
import org.aesh.command.Command;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;

/**
 *
 * @author jdenise@redhat.com
 */
public interface MutableCommandRegistry<C extends Command<CI>,CI extends CommandInvocation> extends CommandRegistry<C,CI> {

    void addCommand(CommandContainer<C,CI> container);

    void addCommand(C command) throws CommandLineParserException;

    void addCommand(Class<C> command) throws CommandLineParserException;

    void addAllCommands(List<C> commands) throws CommandLineParserException;

    void addAllCommandContainers(List<CommandContainer<C,CI>> commands);

    void removeCommand(String name);
}
