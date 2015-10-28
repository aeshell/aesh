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
package org.jboss.aesh.readline.actions;

import org.jboss.aesh.console.Buffer;
import org.jboss.aesh.console.InputProcessor;
import org.jboss.aesh.history.SearchDirection;
import org.jboss.aesh.readline.Action;
import org.jboss.aesh.readline.KeyEvent;
import org.jboss.aesh.readline.SearchAction;
import org.jboss.aesh.terminal.Key;
import org.jboss.aesh.util.ANSI;
import org.jboss.aesh.util.LoggerUtil;

import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ReverseSearchHistory implements SearchAction {

    private boolean searching = false;
    private SearchAction.Status status = Status.SEARCH_NOT_STARTED;
    private KeyEvent inKey;
    private StringBuilder searchArgument;
    private String searchResult;

    private static final Logger LOGGER = LoggerUtil.getLogger(ReverseSearchHistory.class.getName());

    public String name() {
        return "reverse-search-history";
    }

     @Override
    public void input(Action action, KeyEvent key) {
         LOGGER.info("got search, key: "+key);

         if(action == null && Key.isPrintable(key.buffer().array())) {
             if(searchArgument == null)
                 searchArgument = new StringBuilder();
             status = Status.SEARCH_PREV;
             LOGGER.info("appending to searchArgument");
             searchArgument.append((char) key.buffer().array()[0]);
             LOGGER.info("searchArgument: "+searchArgument);
         }
         else if(action instanceof Interrupt) {
             LOGGER.info("interrupting");
             status = Status.SEARCH_INTERRUPT;
         }
         else if(action instanceof Enter) {
             status = Status.SEARCH_END;
         }
         else if(action instanceof ReverseSearchHistory) {
             status = Status.SEARCH_PREV;
         }
         else if(action instanceof DeletePrevChar) {
             status = Status.SEARCH_DELETE;
         }
         else {
             if(key == Key.ESC) {
                 LOGGER.info("got ESC");
                 status = Status.SEARCH_EXIT;
             }
             if(Key.isPrintable(key.buffer().array())) {
                 if(searchArgument == null)
                     searchArgument = new StringBuilder();
                 LOGGER.info("appending to searchArgument");
                 searchArgument.append((char) key.buffer().array()[0]);
                 LOGGER.info("searchArgument: "+searchArgument);
             }
         }
    }

    @Override
    public boolean isSearching() {
        if(status == Status.SEARCH_INTERRUPT || status == Status.SEARCH_END ||
                status == Status.SEARCH_EXIT || status == Status.SEARCH_NOT_STARTED)
            return false;
        else
            return true;
    }

   @Override
    public void apply(InputProcessor inputProcessor) {
       LOGGER.info("processing search");

       if(status == Status.SEARCH_INTERRUPT) {
           inputProcessor.getBuffer().moveCursor(-inputProcessor.getBuffer().getBuffer().getMultiCursor());
           inputProcessor.getBuffer().setBufferLine("");
           inputProcessor.getBuffer().drawLine();
           inputProcessor.getBuffer().out().print(Buffer.printAnsi((inputProcessor.getBuffer().getBuffer().getPrompt().getLength() + 1) + "G"));
           inputProcessor.getBuffer().out().flush();
           searchArgument = null;
           searchResult = null;
       }
       else {
           switch(status) {
               case SEARCH_PREV:
                   if(inputProcessor.getHistory().getSearchDirection() != SearchDirection.REVERSE)
                       inputProcessor.getHistory().setSearchDirection(SearchDirection.REVERSE);
                   LOGGER.info("doing a search with: "+searchArgument);
                   if(searchArgument != null && searchArgument.length() > 0) {
                       String tmpResult = inputProcessor.getHistory().search(searchArgument.toString());
                       LOGGER.info("tmpResult: "+tmpResult);
                       if(tmpResult == null)
                           searchArgument.deleteCharAt(searchArgument.length()-1);
                       else
                           searchResult = tmpResult;
                   }
                   LOGGER.info("result was: "+searchResult);
                   break;
               case SEARCH_NOT_STARTED:
                   LOGGER.info("setting status to search_prev");
                   status = Status.SEARCH_PREV;
                   inputProcessor.getHistory().setSearchDirection(SearchDirection.REVERSE);
                   if(inputProcessor.getBuffer().getBuffer().getLine().length() > 0) {
                       searchArgument = new StringBuilder( inputProcessor.getBuffer().getBuffer().getLine());
                       searchResult = inputProcessor.getHistory().search(searchArgument.toString());
                   }
                   break;
               case SEARCH_DELETE:
                   if(searchArgument != null && searchArgument.length() > 0) {
                       searchArgument.deleteCharAt(searchArgument.length() - 1);
                       LOGGER.info("search argument: "+searchArgument);
                       searchResult = inputProcessor.getHistory().search(searchArgument.toString());
                   }
                   break;
               case SEARCH_END:
                   if(searchResult != null) {
                       inputProcessor.getBuffer().moveCursor(-inputProcessor.getBuffer().getBuffer().getMultiCursor());
                       inputProcessor.getBuffer().setBufferLine( searchResult);
                       inputProcessor.getBuffer().drawLine();
                       inputProcessor.getBuffer().out().println();
                       inputProcessor.getHistory().push(inputProcessor.getBuffer().getBuffer().getLineNoMask());
                       //search.setResult( inputProcessor.getBuffer().getBuffer().getLineNoMask());
                       //search.setFinished(true);
                       inputProcessor.getBuffer().getBuffer().reset();
                       inputProcessor.setReturnValue(searchResult);
                       break;
                   }
                   else {
                       inputProcessor.getBuffer().moveCursor(-inputProcessor.getBuffer().getBuffer().getMultiCursor());
                       inputProcessor.getBuffer().setBufferLine("");
                       inputProcessor.getBuffer().drawLine();
                   }
                   break;
               case SEARCH_EXIT:
                   if(searchResult != null) {
                       inputProcessor.getBuffer().moveCursor(-inputProcessor.getBuffer().getBuffer().getMultiCursor());
                       inputProcessor.getBuffer().setBufferLine(searchResult);
                   }
                   else {
                       inputProcessor.getBuffer().moveCursor(-inputProcessor.getBuffer().getBuffer().getMultiCursor());
                       inputProcessor.getBuffer().setBufferLine("");
                   }
                   break;
           }

           if(!isSearching()) {
               searchArgument = null;
               searchResult = null;
               inputProcessor.getBuffer().drawLine();
               inputProcessor.getBuffer().out().print(Buffer.printAnsi((inputProcessor.getBuffer().getBuffer().getPrompt().getLength() + 1) + "G"));
               inputProcessor.getBuffer().out().flush();
           }
           else {
               LOGGER.info("print the search:");
               if(searchArgument == null || searchArgument.length() == 0) {
                   if(searchResult != null)
                       printSearch("", searchResult, inputProcessor);
                   else
                       printSearch("", "", inputProcessor);
               }
               else {
                   if(searchResult != null && searchResult.length() > 0)
                       printSearch(searchArgument.toString(), searchResult, inputProcessor);
               }
           }
       }
    }

    private void printSearch(String searchTerm, String result, InputProcessor inputProcessor) {
        //cursor should be placed at the index of searchTerm
        int cursor = result.indexOf(searchTerm);

        StringBuilder builder;
        if(inputProcessor.getHistory().getSearchDirection() == SearchDirection.REVERSE)
            builder = new StringBuilder("(reverse-i-search) `");
        else
            builder = new StringBuilder("(forward-i-search) `");
        builder.append(searchTerm).append("': ");
        cursor += builder.length();
        builder.append(result);
        inputProcessor.getBuffer().getBuffer().disablePrompt(true);
        inputProcessor.getBuffer().moveCursor(-inputProcessor.getBuffer().getBuffer().getMultiCursor());
        inputProcessor.getBuffer().out().print(ANSI.CURSOR_START);
        inputProcessor.getBuffer().out().print(ANSI.START + "2K");
        inputProcessor.getBuffer().setBufferLine(builder.toString());
        inputProcessor.getBuffer().moveCursor(cursor);
        inputProcessor.getBuffer().drawLine();
        inputProcessor.getBuffer().getBuffer().disablePrompt(false);
        inputProcessor.getBuffer().out().flush();
    }
}
