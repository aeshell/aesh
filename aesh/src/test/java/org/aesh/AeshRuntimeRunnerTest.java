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
package org.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AeshRuntimeRunnerTest {

    @Test
    public void testCommandResult() throws InterruptedException {
        CommandResult result = AeshRuntimeRunner.builder().command(Bar1Command.class).execute();
        Thread.sleep(200);
        assertEquals(CommandResult.SUCCESS.getResultValue(), result.getResultValue());

        result = AeshRuntimeRunner.builder().command(Bar2Command.class).execute();
        Thread.sleep(200);
        assertEquals(CommandResult.FAILURE.getResultValue(), result.getResultValue());

    }

    @CommandDefinition(name = "bar1", description = "bar1")
    public static class Bar1Command implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.println("Hello from Bar1");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "bar2", description = "bar2")
    public static class Bar2Command implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {
            commandInvocation.println("Hello from Bar2, lets fail");
            return CommandResult.FAILURE;
        }
    }

}
