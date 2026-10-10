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
package examples;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.selector.Selector;
import org.aesh.terminal.utils.ProgramStatus;

/**
 * OSC 7501 program-status reporting: work, progress, approval blocking,
 * resumption, and completion. Reporting is opt-in on the runner; without
 * it every {@code reportProgramStatus} call below is a quiet no-op.
 * <p>
 * Reports travel on the terminal connection, never through command
 * output — try {@code deploy &gt; out.txt} and note the file carries no
 * escape bytes while the terminal still tracks the run.
 */
public class ProgramStatusExample {

    public static void main(String[] args) {
        AeshConsoleRunner.builder()
                .command(DeployCommand.class)
                .enableProgramStatus(true)
                .programStatusAppName("deploy")
                .prompt("[deploy@aesh]$ ")
                .addExitCommand()
                .start();
    }

    @CommandDefinition(name = "deploy", description = "Deploy with progress and approval")
    public static class DeployCommand implements Command<CommandInvocation> {

        @Option(name = "region", description = "Target region", defaultValue = "us-east")
        private String region;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws InterruptedException {
            // Indeterminate work first: no percentage known yet.
            commandInvocation.reportProgramStatus(ProgramStatus.builder(ProgramStatus.State.WORKING)
                    .message("Starting deploy to " + region).build());

            String[] stages = { "Pushing image", "Rolling update", "Health check" };
            for (int i = 0; i < stages.length; i++) {
                Thread.sleep(200);
                commandInvocation.println(stages[i] + "...");
                commandInvocation.reportProgramStatus(ProgramStatus.builder(ProgramStatus.State.WORKING)
                        .progress((i + 1) * 100 / stages.length)
                        .message(stages[i]).build());
            }

            // Blocked: needs a human decision. The explicit report names the
            // kind; the prompt itself is an ordinary blocking read.
            commandInvocation.reportProgramStatus(ProgramStatus.builder(ProgramStatus.State.BLOCKED)
                    .kind(ProgramStatus.BlockedKind.PERMISSION)
                    .message("Approve deploy to " + region + " (production)?").build());
            boolean approved = Selector.confirm(commandInvocation.getShell(),
                    "Approve deploy to " + region + " (production)?", false);
            if (!approved) {
                commandInvocation.println("Deploy aborted.");
                return CommandResult.FAILURE;
            }

            // Explicit resumption: back to working after the decision.
            commandInvocation.reportProgramStatus(ProgramStatus.builder(ProgramStatus.State.WORKING)
                    .message("Deploy approved, finishing...").build());
            Thread.sleep(200);
            commandInvocation.println("Deployed to " + region + ".");
            // Success completes automatically with a done record.
            return CommandResult.SUCCESS;
        }
    }
}
