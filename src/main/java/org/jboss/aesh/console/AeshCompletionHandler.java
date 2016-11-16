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
package org.jboss.aesh.console;

import org.aesh.readline.Buffer;
import org.aesh.readline.InputProcessor;
import org.aesh.readline.action.mappings.ActionMapper;
import org.aesh.readline.alias.Alias;
import org.aesh.readline.alias.AliasManager;
import org.aesh.readline.completion.Completion;
import org.aesh.readline.completion.CompletionHandler;
import org.aesh.terminal.formatting.TerminalString;
import org.aesh.tty.Connection;
import org.aesh.util.Config;
import org.aesh.util.LoggerUtil;
import org.jboss.aesh.complete.AeshCompleteOperation;
import org.jboss.aesh.console.operator.ControlOperatorParser;
import org.jboss.aesh.console.operator.RedirectionCompletion;
import org.jboss.aesh.parser.Parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCompletionHandler implements CompletionHandler<AeshCompleteOperation> {

    private final Connection connection;
    private volatile boolean enabled = true;

    private final AeshContext aeshContext;
    private boolean askDisplayCompletion = false;
    private int displayCompletionSize = 100;
    private final List<Completion> completionList;
    private AliasManager aliasManager;
    private final boolean doLogging;

    private static final Logger LOGGER = LoggerUtil.getLogger(AeshCompletionHandler.class.getName());

    public AeshCompletionHandler(AeshContext aeshContext, Connection connection,
                                 boolean doLogging) {
        completionList = new ArrayList<>();
        this.aeshContext = aeshContext;
        this.connection = connection;
        this.doLogging = doLogging;
    }

    @Override
    public void addCompletion(Completion completion) {
        if(completion != null)
            completionList.add(completion);
    }

    @Override
    public void removeCompletion(Completion completion) {
        completionList.remove(completion);
    }

    @Override
    public void clear() {
       completionList.clear();
    }

    @Override
    public boolean doAskDisplayCompletion() {
        return askDisplayCompletion;
    }

    @Override
    public void setAskDisplayCompletion(boolean askDisplayCompletion) {
        this.askDisplayCompletion = askDisplayCompletion;
    }

    @Override
    public void setAskCompletionSize(int size) {
        displayCompletionSize = size;
    }

    @Override
    public int getAskCompletionSize() {
        return displayCompletionSize;
    }

    public void setAliasManager(AliasManager aliasManager) {
        this.aliasManager = aliasManager;
    }

     /**
     * Display possible completions.
     * 1. Find all possible completions
     * 2. If we find only one, display it.
     * 3. If we find more than one, display them,
     *    but not more than 100 at once
     */
    @Override
    public void complete(InputProcessor inputProcessor) {
        Buffer buffer = inputProcessor.getBuffer().getBuffer();
        if(!enabled)
            return;

        if(completionList.size() < 1)
            return;

        List<AeshCompleteOperation> possibleCompletions = new ArrayList<>();
        int pipeLinePos = 0;
        boolean redirect = false;
        String line = buffer.asString();
        if(ControlOperatorParser.doStringContainPipelineOrEnd(buffer.asString())) {
            pipeLinePos =  ControlOperatorParser.findLastPipelineAndEndPositionBeforeCursor(line, buffer.getCursor());
        }
        if(ControlOperatorParser.findLastRedirectionPositionBeforeCursor(line, buffer.getCursor()) > pipeLinePos) {
            pipeLinePos = 0;
            redirect = true;
        }

        for(int i=0; i < completionList.size(); i++) {
            if(redirect && !completionList.get(i).getClass().equals(RedirectionCompletion.class)) {
                break;
            }
            AeshCompleteOperation co;
            if(pipeLinePos > 0) {
                co = findAliases(line.substring(pipeLinePos, buffer.getCursor()), buffer.getCursor() - pipeLinePos);
            }
            else {
                co = findAliases(line, buffer.getCursor());
            }

            completionList.get(i).complete(co);

            if(co.getCompletionCandidates() != null && co.getCompletionCandidates().size() > 0)
                possibleCompletions.add(co);
        }

        if(doLogging)
            LOGGER.info("Found completions: "+possibleCompletions);

        if(possibleCompletions.size() == 0) {
            //do nothing
        }
        // only one hit, do a completion
        else if(possibleCompletions.size() == 1 &&
                possibleCompletions.get(0).getCompletionCandidates().size() == 1) {
            //some formatted completions might not be valid and shouldnt be displayed
            displayCompletion(
                    possibleCompletions.get(0).getFormattedCompletionCandidatesTerminalString().get(0),
                    buffer, inputProcessor,
                    possibleCompletions.get(0).hasAppendSeparator(),
                    possibleCompletions.get(0).getSeparator());
        }
        // more than one hit...
        else {

            String startsWith = "";

            if(!possibleCompletions.get(0).isIgnoreStartsWith())
                startsWith = Parser.findStartsWithOperation(possibleCompletions);

            if(startsWith.length() > 0 ) {
                if(startsWith.contains(" ") && !possibleCompletions.get(0).doIgnoreNonEscapedSpace())
                    displayCompletion(new TerminalString(Parser.switchSpacesToEscapedSpacesInWord(startsWith), true),
                            buffer, inputProcessor,
                            false, possibleCompletions.get(0).getSeparator());
                else
                    displayCompletion(new TerminalString(startsWith, true), buffer, inputProcessor,
                            false, possibleCompletions.get(0).getSeparator());
            }
                // display all
                // check size
            else {
                List<TerminalString> completions = new ArrayList<>();
                for(int i=0; i < possibleCompletions.size(); i++)
                    completions.addAll(possibleCompletions.get(i).getCompletionCandidates());

                if(completions.size() > 100) {
                    //if(displayCompletion) {
                     if(askDisplayCompletion) {
                        displayCompletions(completions, buffer, inputProcessor);
                        //displayCompletion = false;
                         askDisplayCompletion = false;
                    }
                    else {
                        askDisplayCompletion = true;
                        connection.write(Config.getLineSeparator() + "Display all " + completions.size() + " possibilities? (y or n)");
                    }
                }
                // display all
                else {
                    displayCompletions(completions, buffer, inputProcessor);
                }
            }
        }
    }

    @Override
    public void setAliasHandler(Function<Buffer, AeshCompleteOperation> aliasHandler) {

    }

    @Override
    public void addCompletions(List<Completion> completions) {
        if(completions != null && completions.size() > 0)
            completionList.addAll(completions);
    }

    /**
     * Display the completion string in the terminal.
     * If !completion.startsWith(buffer.getLine()) the completion will be added to the line,
     * else it will replace whats at the buffer line.
     *
     * @param completion partial completion
     * @param appendSpace if its an actual complete
     */
    private void displayCompletion(TerminalString completion, Buffer buffer, InputProcessor inputProcessor,
                                   boolean appendSpace, char separator) {

        LOGGER.info("completion: "+completion.getCharacters()+" and buffer: "+buffer.asString());
        if(completion.getCharacters().startsWith(buffer.asString())) {
            ActionMapper.mapToAction("backward-kill-word").accept(inputProcessor);
            //consoleBuffer.performAction(new PrevWordAction(buffer.getMultiCursor(), Action.DELETE, EditMode.Mode.EMACS));
            //buffer.write(completion.getCharacters());
            inputProcessor.getBuffer().writeString(completion.toString());

            //only append space if its an actual complete, not a partial
        }
        else {
            inputProcessor.getBuffer().writeString(completion.toString());
            //buffer.insert(completion.toString());
        }
        if(appendSpace) { // && fullCompletion.startsWith(buffer.getLine())) {
            inputProcessor.getBuffer().writeChar(separator);
            //buffer.write(separator);
        }
        /*
        int width = connection.size().getWidth();
        if(completion.getCharacters().startsWith(buffer.asString())) {
            buffer.replace(connection.stdoutHandler(), completion.getCharacters(), width);
        }
        else {
            buffer.insert(connection.stdoutHandler(), completion.getCharacters(), width);
        }
        if(appendSpace) { // && fullCompletion.startsWith(buffer.getLine())) {
            buffer.insert(connection.stdoutHandler(), separator, width);
        }
        */
    }

    /**
     * Display all possible completions
     *
     * @param completions all completion items
     */
    private void displayCompletions(List<TerminalString> completions, Buffer buffer,
                                    InputProcessor inputProcessor) {
        Collections.sort(completions);
        //printNewline reset cursor pos, so we need to store it

        inputProcessor.getBuffer().writeOut(Config.CR);
        inputProcessor.getBuffer().writeOut(Parser.formatDisplayListTerminalString(completions,
                inputProcessor.getBuffer().getSize().getHeight(), inputProcessor.getBuffer().getSize().getWidth()));

        buffer.setIsPromptDisplayed(false);
        inputProcessor.getBuffer().drawLine();
        /*
        int oldCursorPos = buffer.getCursor();
        connection.write(Config.getLineSeparator());
        //buffer.setCursor(oldCursorPos);
        Size size = connection.size();
        connection.write(Parser.formatDisplayListTerminalString(completions,
                size.getHeight(), size.getWidth()));
        //inputProcessor.getBuffer().displayPrompt();
        buffer.replace(connection.stdoutHandler(), buffer.asString(), connection.size().getWidth());
        //out.print(buffer.getLine());
        //if we do a complete and the cursor is not at the end of the
        //buffer we need to move it to the correct place
        //out.flush();
        //inputProcessor.getBuffer().syncCursor();
        */
    }

    private AeshCompleteOperation findAliases(String buffer, int cursor) {
        if(aliasManager != null) {
            String command = Parser.findFirstWord(buffer);
            Optional<Alias> alias = aliasManager.getAlias(command);
            if(alias.isPresent()) {
                return new AeshCompleteOperation(aeshContext, alias.get().getValue()+buffer.substring(command.length()),
                        cursor+(alias.get().getValue().length()-command.length()));
            }
        }

        return new AeshCompleteOperation(aeshContext, buffer, cursor);
    }
}
