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

import org.aesh.command.validator.OptionValidator;
import org.aesh.command.CommandDefinition;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;
import org.aesh.readline.AeshContext;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.CommandResult;
import org.aesh.command.validator.ValidatorInvocation;

import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@CommandDefinition(name = "test", description = "a simple test")
public class TestPopulator5 implements Command<CommandInvocation> {

    @Option(shortName = 'v', validator = LongOptionValidator.class)
    private Long veryLong;

    @OptionList
    private Set<String> basicSet;

    @OptionGroup(shortName = 'D', description = "define properties")
    private TreeMap<String, Integer> define;

    @Option(shortName = 'c', converter = CurrencyConverter.class)
    private Currency currency;

    @OptionList(validator = LongOptionValidator.class, valueSeparator = ';')
    private List<Long> longs;

    @OptionList(name = "strings")
    private List<String> strings;

    @Option(name = "bar", hasValue = false)
    private Boolean bar;

    @Arguments
    private Set<String> arguments;

    public TestPopulator5() {
    }

    public Set<String> getBasicSet() {
        return basicSet;
    }

    public Map<String, Integer> getDefine() {
        return define;
    }

    public Set<String> getArguments() {
        return arguments;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Long getVeryLong() {
        return veryLong;
    }

    public List<Long> getLongs() {
        return longs;
    }

    public List<String> getStrings() {
        return strings;
    }

    public Boolean getBar() {
        return bar;
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        return CommandResult.SUCCESS;
    }

    public class LongOptionValidator implements OptionValidator {
        @Override
        public void validate(ValidatorInvocation validatorInvocation) throws OptionValidatorException {
            Long value = (Long) validatorInvocation.getValue();
            if(value < 0 || value > 100)
                throw new OptionValidatorException("value must be between 0 and 100");
        }
    }

    public class LongValidatorInvocation implements ValidatorInvocation<Long, Object> {

        private final Long value;

        public LongValidatorInvocation(Long value) {
            this.value = value;
        }

        @Override
        public Long getValue() {
            return value;
        }

        @Override
        public Object getCommand() {
            return null;
        }

        @Override
        public AeshContext getAeshContext() {
            return null;
        }
    }

}


