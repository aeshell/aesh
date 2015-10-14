/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jboss.aesh.terminal.api;

public class Size {

    private int height;
    private int width;

    public Size() {
    }

    public Size(int height, int width) {
        this.height = height;
        this.width = width;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = (short) width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = (short) height;
    }

    public void copy(Size size) {
        setWidth(size.getWidth());
        setHeight(size.getHeight());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Size size = (Size) o;

        if (height != size.height) return false;
        return width == size.width;

    }

    @Override
    public int hashCode() {
        int result = height;
        result = 31 * result + width;
        return result;
    }

    @Override
    public String toString() {
        return "Size[" +
                "cols=" + width +
                ", rows=" + height +
                ']';
    }
}
