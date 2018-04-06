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
import org.aesh.command.CommandException;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.option.OptionList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@CommandDefinition(name = "test", description = "a simple test")
public class TestPopulator2<CI extends CommandInvocation> implements Command<CI> {

    @OptionList(shortName = 'b')
    private Set<String> basicSet;

    @OptionList(shortName = 'a')
    private List<Integer> basicList;

    @OptionList(shortName = 'i')
    private ArrayList<Short> implList;

    /*
    @OptionGroup(shortName = 'D', description = "define properties")
    public Map<String, String> define;
    */

    public TestPopulator2() {
    }

    public Set<String> getBasicSet() {
        return basicSet;
    }

    public List<Integer> getBasicList() {
        return basicList;
    }

    public List<Short> getImplList() {
        return implList;
    }

    @Override
    public CommandResult execute(CI commandInvocation) throws CommandException, InterruptedException {
        return CommandResult.SUCCESS;
    }
}