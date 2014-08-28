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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ackage org.jboss.aesh.console.registry;

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
