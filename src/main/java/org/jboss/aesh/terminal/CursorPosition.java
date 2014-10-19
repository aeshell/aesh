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
        return row == that.row;

    }

    @Override
    public int hashCode() {
        int result = row;
        result = 31 * result + column;
        return result;
    }

}
