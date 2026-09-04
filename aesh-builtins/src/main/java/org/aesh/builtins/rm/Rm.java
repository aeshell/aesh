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
package org.aesh.builtins.rm;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.io.Resource;
import org.aesh.terminal.Key;

import java.util.List;

/**
 * A simple rm command.
 *
 * @author <a href="mailto:00hf11@gmail.com">Helio Frota</a>
 */
@CommandDefinition(name = "rm", description = "remove files or directories.")
public class Rm implements Command<CommandInvocation> {

    @Option(shortName = 'h', name = "help", hasValue = false, description = "display this help and exit")
    private boolean help;

    @Option(shortName = 'd', name = "dir", hasValue = false, description = "remove empty directories")
    private boolean dir;

    @Option(shortName = 'i', name = "interactive", hasValue = false, description = "prompt before every removal")
    private boolean interactive;

    @Option(shortName = 'v', name = "verbose", hasValue = false, description = "explain what is being done")
    private boolean verbose;

    @Arguments(description = "files or directories to remove")
    private List<Resource> args;

    @Override
    public CommandResult execute(CommandInvocation ci) throws InterruptedException {
        if (help || args == null || args.isEmpty()) {
            ci.getShell().writeln(ci.getHelpInfo("rm"));
            return CommandResult.SUCCESS;
        }

        Resource currentDir = ci.getConfiguration().getAeshContext().getCurrentWorkingDirectory();
        for (Resource r : args) {
            Resource res = r.resolve(currentDir).get(0);
            if (dir)
                rmDir(res, ci);
            else
                rmFile(res, ci);
        }

        return CommandResult.SUCCESS;
    }

    private void rmFile(Resource r, CommandInvocation ci) throws InterruptedException {
        if (r.exists()) {
            if (r.isLeaf()) {
                if (interactive) {
                    ci.getShell().writeln("remove regular file '" + r.getName() + "' ? (y/n)");
                    if (Key.y.equalTo(ci.input()))
                        r.delete();
                }
                else
                    r.delete();
                if (verbose)
                    ci.getShell().writeln("removed '" + r.getName() + "'");
            }
            else if (r.isDirectory()) {
                ci.getShell().writeln("cannot remove '" + r.getName() + "': Is a directory");
            }
        }
    }

    private void rmDir(Resource r, CommandInvocation ci) throws InterruptedException {
        if (r.exists() && r.isDirectory()) {
            if (interactive) {
                ci.getShell().writeln("remove directory '" + r.getName() + "' ? (y/n)");
                if (Key.y.equalTo(ci.input()))
                    r.delete();
            }
            else
                r.delete();

            if (verbose)
                ci.getShell().writeln("removed directory: '" + r.getName() + "'");
        }
    }
}
