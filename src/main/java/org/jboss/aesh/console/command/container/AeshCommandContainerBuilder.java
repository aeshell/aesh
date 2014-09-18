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
package org.jboss.aesh.console.command.container;

import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.parser.ParserGenerator;
import org.jboss.aesh.console.command.Command;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandContainerBuilder implements CommandContainerBuilder {

    @Override
    public CommandContainer build(Command command) {
        try {
            return ParserGenerator.generateCommandLineParser(command);
        }
        catch (CommandLineParserException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public CommandContainer build(Class<? extends Command> command) {
        try {
            return ParserGenerator.generateCommandLineParser(command);
        }

        catch (CommandLineParserException e) {
            e.printStackTrace();
            return null;
        }
    }
}
