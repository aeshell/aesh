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
package org.jboss.aesh.console.command.container;

import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.parser.AeshCommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.console.command.Command;

import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 * @author <a href="mailto:danielsoro@gmail.com">Daniel Cunha (soro)</a>
 */
public class AeshCommandContainer<C extends Command> extends DefaultCommandContainer<C> {

    private CommandLineParser<C> parser;
    private String errorMessage;

    public AeshCommandContainer(CommandLineParser parser) {
        if (parser != null && parser.getProcessedCommand() != null) {
            this.parser = parser;
        }
    }

    public AeshCommandContainer(ProcessedCommand<C> processedCommand) {
        parser = new AeshCommandLineParser<>(processedCommand );
    }

    public AeshCommandContainer(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public CommandLineParser<C> getParser() {
        return parser;
    }

    @Override
    public boolean haveBuildError() {
        return errorMessage != null;
    }

    @Override
    public String getBuildErrorMessage() {
        return errorMessage;
    }

    @Override
    public void close() {

    }

    @Override
    public List<CommandLineParser<? extends Command>> getChildren() {
        return getParser().getAllChildParsers();
    }

    public void addChild(CommandContainer<?> commandContainer) {
        getParser().addChildParser(commandContainer.getParser());
    }

}
