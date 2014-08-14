/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.registry;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.MutableCommandRegistry;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

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

        List<String> commands = registry.findAllCommandNames("fo");
        assertEquals(1, commands.size());
        assertEquals("foo", commands.get(0));

        commands = registry.findAllCommandNames("foo");
        assertEquals(1, commands.size());
        assertEquals("foo", commands.get(0));

        commands = registry.findAllCommandNames("");
        assertEquals(4, commands.size());
        assertTrue(commands.contains("bar"));
        assertTrue(commands.contains("group"));

        commands = registry.findAllCommandNames("group he");
        assertEquals(1, commands.size());
        assertEquals("group help", commands.get(0));

        commands = registry.findAllCommandNames("group ");
        assertEquals(1, commands.size());
        assertEquals("group help", commands.get(0));

        commands = registry.findAllCommandNames("group   ");
        assertEquals(1, commands.size());
        assertEquals("group   help", commands.get(0));

        commands = registry.findAllCommandNames("group help ");
        assertEquals(0, commands.size());

    }


    @CommandDefinition(name = "foo", description = "")
    public class Command1 implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "bar", description = "")
    public class Command2 implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "help", description = "")
    public class Command3 implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "group", description = "", groupCommands = {Command3.class})
    public class GroupCommand1 implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

}
