/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

import org.jboss.aesh.console.Buffer;

/**
 * Define the position of the cursor in a terminal
 * 1:1 is upper left corner.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CursorPosition {

    private int row;
    private int column;

    public CursorPosition(int row, int column) {
        setRow(row);
        setColumn(column);
    }

    public void setPosition(CursorPosition cp) {
        setRow(cp.getRow());
        setColumn(cp.getColumn());
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public void move(int row, int column) {
        this.row = this.row + row;
        this.column = this.column + column;
    }

    public char[] asAnsi() {
        return Buffer.printAnsi(row+";"+column+"H");
    }

    @Override
    public String toString() {
        return "CursorPosition{" +
                "row=" + row +
                ", column=" + column +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CursorPosition)) return false;

        CursorPosition that = (CursorPosition) o;

        if (column != that.column) return false;
        if (row != that.row) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = row;
        result = 31 * result + column;
        return result;
    }

}
