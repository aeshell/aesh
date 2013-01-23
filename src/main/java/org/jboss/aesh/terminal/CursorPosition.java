/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CursorPosition {

    private int y;
    private int x;

    public CursorPosition(int y, int x) {
        setY(y);
        setX(x);
    }

    public void setPosition(CursorPosition cp) {
        setY(cp.getY());
        setX(cp.getX());
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    @Override
    public String toString() {
        return "CursorPosition{" +
                "y=" + y +
                ", x=" + x +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CursorPosition)) return false;

        CursorPosition that = (CursorPosition) o;

        if (x != that.x) return false;
        if (y != that.y) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = y;
        result = 31 * result + x;
        return result;
    }

}
