/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
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
package org.jboss.jreadline.edit;

import org.jboss.jreadline.console.Config;
import org.jboss.jreadline.edit.actions.Action;
import org.jboss.jreadline.edit.actions.Operation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Trying to follow the Emacs mode GNU Readline impl found here:
 * http://cnswww.cns.cwru.edu/php/chet/readline/readline.html
 *
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class EmacsEditMode implements EditMode {

    private Action mode = Action.EDIT;

    private List<KeyOperation> operations;
    private List<KeyOperation> currentOperations = new ArrayList<KeyOperation>();
    private int operationLevel = 0;

    public EmacsEditMode(List<KeyOperation> operations) {
        this.operations = operations;
    }

    @Override
    public Operation parseInput(int[] in) {

        int input = in[0];
        if(Config.isOSPOSIXCompatible() && in.length > 1) {
            KeyOperation ko = KeyOperationManager.findOperation(operations, in);
            if(ko != null) {
                //clear current operations to make sure that everything works as expected
                currentOperations.clear();
                currentOperations.add(ko);
            }
        }
        else {
            //if we're in the middle of parsing a sequence input
            //currentOperations.add(KeyOperationManager.findOperation(operations, input));
            if(operationLevel > 0) {
                Iterator<KeyOperation> operationIterator = currentOperations.iterator();
                while(operationIterator.hasNext())
                    if(input != operationIterator.next().getKeyValues()[operationLevel])
                        operationIterator.remove();

            }
            // parse a first sequence input
            else {
                for(KeyOperation ko : operations)
                    if(input == ko.getFirstValue())
                        currentOperations.add(ko);
            }
        }

        //search mode need special handling
        if(mode == Action.SEARCH) {
            if(currentOperations.size() == 1) {
                if(currentOperations.get(0).getOperation() == Operation.NEW_LINE) {
                    mode = Action.EDIT;
                    currentOperations.clear();
                    return Operation.SEARCH_END;
                }
                else if(currentOperations.get(0).getOperation() == Operation.SEARCH_PREV) {
                    currentOperations.clear();
                    return Operation.SEARCH_PREV_WORD;
                }
                else if(currentOperations.get(0).getOperation() == Operation.SEARCH_NEXT_WORD) {
                    currentOperations.clear();
                    return Operation.SEARCH_NEXT_WORD;
                }
                else if(currentOperations.get(0).getOperation() == Operation.DELETE_PREV_CHAR) {
                    currentOperations.clear();
                    return Operation.SEARCH_DELETE;
                }
                // TODO: unhandled operation, should parse better
                else {
                    currentOperations.clear();
                    return Operation.NO_ACTION;
                }
            }
            //if we got more than one we know that it started with esc
            else if(currentOperations.size() > 1) {
                mode = Action.EDIT;
                currentOperations.clear();
                return Operation.SEARCH_EXIT;
            }
            // search input
            else {
                currentOperations.clear();
                return Operation.SEARCH_INPUT;
            }
        } // end search mode
        else {
            // process if we have any hits...
            if(currentOperations.isEmpty()) {
                //if we've pressed meta-X, where X is not caught we just disable the output
                if(operationLevel > 0) {
                    operationLevel = 0;
                    currentOperations.clear();
                    return Operation.NO_ACTION;
                }
                else
                    return Operation.EDIT;
            }
            else if(currentOperations.size() == 1) {
                //need to check if this one operation have more keys
                int level = operationLevel+1;
                if(in.length > level)
                    level = in.length;
                if(currentOperations.get(0).getKeyValues().length > level) {
                    operationLevel++;
                    return Operation.NO_ACTION;
                }
                Operation currentOperation = currentOperations.get(0).getOperation();
                if(currentOperation == Operation.SEARCH_PREV ||
                        currentOperation == Operation.SEARCH_NEXT_WORD)
                    mode = Action.SEARCH;

                operationLevel = 0;
                currentOperations.clear();

                return currentOperation;
            }
            else {
                operationLevel++;
                return Operation.NO_ACTION;
            }
        }
    }

    @Override
    public Action getCurrentAction() {
        return mode;
    }

    @Override
    public Mode getMode() {
        return Mode.EMACS;
    }
}
