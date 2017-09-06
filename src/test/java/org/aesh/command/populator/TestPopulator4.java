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
package org.aesh.command.populator;

import org.aesh.command.CommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.Command;
import org.aesh.command.CommandResult;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.aesh.command.CommandException;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;

@CommandDefinition(name = "test", description = "a simple test")
public class TestPopulator4<CI extends CommandInvocation> implements Command<CI> {

    @Option
    private Long veryLong;

    @OptionList
    private Set<String> basicSet;

    @OptionGroup(shortName = 'D', description = "define properties")
    private TreeMap<String, Integer> define;

    @Arguments(required = true)
    private Set<File> arguments;

    public TestPopulator4() {
    }

    public Set<String> getBasicSet() {
        return basicSet;
    }

    public Map<String, Integer> getDefine() {
        return define;
    }

    public Set<File> getArguments() {
        return arguments;
    }

    @Override
    public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
        return CommandResult.SUCCESS;
    }
}
