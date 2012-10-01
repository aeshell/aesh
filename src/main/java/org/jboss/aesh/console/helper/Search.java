/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.helper;

import org.jboss.aesh.edit.actions.Operation;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Search {

    private StringBuilder searchTerm;
    private Operation operation;
    private String result;
    private int input;
    private boolean finished;


    public Search(Operation operation, int input) {
        setOperation(operation);
        setSearchTerm(new StringBuilder());
        setResult(null);
        setInput(input);
    }

    public StringBuilder getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = new StringBuilder(searchTerm);
    }

    public void setSearchTerm(StringBuilder searchTerm) {
        this.searchTerm = searchTerm;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getInput() {
        return input;
    }

    public void setInput(int input) {
        this.input = input;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

}
