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
 */
package org.aesh.builtins.grep;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.builtins.common.AeshTestCommons;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class GrepTest extends AeshTestCommons {

    @Test
    public void testGrep() throws IOException, CommandRegistryException {
        prepare(Grep.class);
        pushToOutput("grep -i 'foo' /tmp\n");
        finish();
    }

    @CommandDefinition(name = "slow", description = "producer that sleeps before first output")
    public static class SlowCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation ci) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return CommandResult.FAILURE;
            }
            ci.println("alpha one\nbeta two\nalpha three");
            return CommandResult.SUCCESS;
        }
    }

    @Test
    public void testGrepSlowUpstreamPipe() throws IOException, CommandRegistryException {
        // The producer has not flushed anything when grep starts; grep must
        // block reading to EOF rather than report "no file or input given"
        // based on a point-in-time available() check (#637).
        prepare(SlowCommand.class, Grep.class);
        pushToOutput("slow | grep two\n");
        String output = getStream();
        assertTrue("Slow pipe output should contain the match, got: " + output,
                output.contains("beta two"));
        assertFalse("Slow pipe output must not report missing input, got: " + output,
                output.contains("no file or input given"));
        finish();
    }
}
