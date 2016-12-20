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
package org.aesh.console.registry;

import org.aesh.cl.Option;
import org.aesh.console.command.invocation.CommandInvocation;
import org.aesh.cl.CommandDefinition;
import org.aesh.cl.GroupCommandDefinition;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.command.Command;
import org.aesh.console.command.CommandResult;
import org.aesh.console.command.registry.MutableCommandRegistry;
import org.aesh.terminal.formatting.TerminalString;
import org.junit.Test;

import org.aesh.console.command.CommandException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class MutableCommandRegistryTest {

    @Test
    public void testFindCommandNames() {

        MutableCommandRegistry registry = new MutableCommandRegistry();
        registry.addCommand(Command1.class);
        registry.addCommand(Command2.class);
        registry.addCommand(Command3.class);
        registry.addCommand(GroupCommand1.class);

        AeshCompleteOperation co = new AeshCompleteOperation(null, "fo", 3);
        registry.completeCommandName(co);
        assertEquals(1, co.getCompletionCandidates().size());
        assertEquals("foo", co.getCompletionCandidates().get(0).toString());

        co = new AeshCompleteOperation(null, "foo", 4);
        registry.completeCommandName(co);
        assertEquals(1, co.getCompletionCandidates().size());
        assertEquals("foo", co.getCompletionCandidates().get(0).toString());

        co = new AeshCompleteOperation(null, "", 0);
        registry.completeCommandName(co);
        assertEquals(4, co.getCompletionCandidates().size());
        assertTrue(co.getCompletionCandidates().contains(new TerminalString("bar", true)));
        assertTrue(co.getCompletionCandidates().contains(new TerminalString("group", true)));

        co = new AeshCompleteOperation(null, "group he", 0);
        registry.completeCommandName(co);
        assertEquals(1, co.getCompletionCandidates().size());
        assertEquals("group help", co.getCompletionCandidates().get(0).toString());

        co = new AeshCompleteOperation(null, "group ", 0);
        registry.completeCommandName(co);
        assertEquals(1, co.getCompletionCandidates().size());
        assertEquals("group help", co.getCompletionCandidates().get(0).toString());

        co = new AeshCompleteOperation(null, "group   ", 0);
        registry.completeCommandName(co);
        assertEquals(1, co.getCompletionCandidates().size());
        assertEquals("group   help", co.getCompletionCandidates().get(0).toString());

        co = new AeshCompleteOperation(null, "group help ", 0);
        registry.completeCommandName(co);
        assertEquals(0, co.getCompletionCandidates().size());

    }


    @CommandDefinition(name = "foo", description = "")
    public class Command1 implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "bar", description = "")
    public class Command2 implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "help", description = "")
    public class Command3 implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "group", description = "", groupCommands = {Command3.class})
    public class GroupCommand1 implements Command {

        @Option
        private boolean help;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

}
