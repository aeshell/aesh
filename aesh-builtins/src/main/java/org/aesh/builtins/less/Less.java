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
package org.aesh.builtins.less;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.man.AeshFileDisplayer;
import org.aesh.command.man.FileParser;
import org.aesh.command.man.TerminalPage;
import org.aesh.command.option.Arguments;
import org.aesh.io.Resource;
import org.aesh.terminal.utils.ANSI;
import org.aesh.terminal.utils.Config;

/**
 * A less implementation for Æsh.
 * Displays file content with paging, search, and navigation.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Less_(Unix)">Less (Unix)</a>
 * @author Aesh team
 */
@CommandDefinition(name = "less", description = "Display file content with paging")
public class Less extends AeshFileDisplayer {

    @Arguments(description = "File to display")
    List<Resource> arguments;

    private SimpleFileParser loader;

    public Less() {
        super();
    }

    public void setFile(File file) throws IOException {
        loader.setFile(file);
    }

    public void setFile(String filename) throws IOException {
        loader.setFile(filename);
    }

    public void setInput(String input) {
        loader.readPageAsString(input);
    }

    public void setFile(InputStream inputStream, String fileName) {
        loader.setFile(inputStream, fileName);
    }

    @Override
    public FileParser getFileParser() {
        return loader;
    }

    @Override
    public void displayBottom() throws IOException {
        if (getSearchStatus() == TerminalPage.Search.SEARCHING) {
            clearBottomLine();
            writeToConsole("/" + getSearchWord());
        } else if (getSearchStatus() == TerminalPage.Search.NOT_FOUND) {
            clearBottomLine();
            writeToConsole(ANSI.INVERT_BACKGROUND + "Pattern not found (press RETURN)" + ANSI.DEFAULT_TEXT);
        } else if (getSearchStatus() == TerminalPage.Search.RESULT) {
            writeToConsole(":" + getSearchWord());
        } else if (getSearchStatus() == TerminalPage.Search.NO_SEARCH) {
            if (isAtBottom())
                writeToConsole(ANSI.INVERT_BACKGROUND + "(END)" + ANSI.DEFAULT_TEXT);
            else
                writeToConsole(":");
        }
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        setCommandInvocation(commandInvocation);
        loader = new SimpleFileParser();

        // Check for piped stdin first
        InputStream stdin = commandInvocation.getStdin();
        if (stdin != null) {
            try {
                setInput(readStream(stdin));
                afterAttach();
                return CommandResult.SUCCESS;
            } catch (IOException ex) {
                throw new CommandException(ex);
            }
        }

        if (arguments != null && arguments.size() > 0) {
            Resource f = arguments.get(0)
                    .resolve(commandInvocation.getConfiguration().getAeshContext()
                            .getCurrentWorkingDirectory())
                    .get(0);
            if (f.isLeaf()) {
                try {
                    setFile(f.read(), f.getName());
                    afterAttach();
                } catch (IOException e) {
                    throw new CommandException("Failed to read file: " + f.getAbsolutePath(), e);
                }
            } else if (f.isDirectory()) {
                commandInvocation.println(f.getAbsolutePath() + ": is a directory");
            } else {
                commandInvocation.println(f.getAbsolutePath() + ": No such file or directory");
            }
        } else {
            commandInvocation.println("Missing filename (\"less --help\" for help)");
        }

        return CommandResult.SUCCESS;
    }

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append(Config.getLineSeparator());
            }
        }
        return builder.toString();
    }
}
