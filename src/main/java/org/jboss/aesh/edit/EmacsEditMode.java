/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit;

import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;

/**
 * Trying to follow the Emacs mode GNU Readline impl found here:
 * http://cnswww.cns.cwru.edu/php/chet/readline/readline.html
 *
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class EmacsEditMode implements EditMode {

    private Action mode = Action.EDIT;

    private KeyOperationManager operationManager;

    public EmacsEditMode(KeyOperationManager operations) {
        this.operationManager = operations;
    }

    @Override
    public Operation parseInput(Key in, String buffer) {

        KeyOperation currentOperation = operationManager.findOperation(in);
        if(currentOperation != null)
            return findOperation(currentOperation, buffer);
        else if (mode == Action.SEARCH) {
            if(in == Key.ESC) {
                mode = Action.EDIT;
                return Operation.SEARCH_EXIT;
            }
            else
                return Operation.SEARCH_INPUT;
        }
        else if(in == Key.ESC)
            return Operation.NO_ACTION;
        else
            return Operation.EDIT;
    }

    private Operation findOperation(KeyOperation currentOperation, String buffer) {
        //search mode need special handling
        if(mode == Action.SEARCH) {
                if(currentOperation.getOperation() == Operation.NEW_LINE) {
                    mode = Action.EDIT;
                    return Operation.SEARCH_END;
                }
                else if(currentOperation.getOperation() == Operation.SEARCH_PREV) {
                    return Operation.SEARCH_PREV_WORD;
                }
                else if(currentOperation.getOperation() == Operation.SEARCH_NEXT_WORD) {
                    return Operation.SEARCH_NEXT_WORD;
                }
                else if(currentOperation.getOperation() == Operation.DELETE_PREV_CHAR) {
                    return Operation.SEARCH_DELETE;
                }
                //if we got more than one we know that it started with esc
                // search input
                else {
                    return Operation.SEARCH_INPUT;
                }
        } // end search mode
        else {
            // process if we have any hits...
            Operation operation = currentOperation.getOperation();
            if(operation == Operation.SEARCH_PREV ||
                    operation == Operation.SEARCH_NEXT_WORD)
                mode = Action.SEARCH;

            //if ctrl-d is pressed on an empty line we need to return logout
            //else return delete next char
            if(currentOperation.equals(Operation.EXIT)) {
                if(buffer.isEmpty())
                    return operation;
                else
                    return Operation.DELETE_NEXT_CHAR;
            }

            return operation;
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
