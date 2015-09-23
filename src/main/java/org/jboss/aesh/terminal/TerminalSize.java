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

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TerminalSize {

    private int height;
    private int width;


    /**
     * Initializes a TerminalSize with height and width.
     *
     * @param height int
     * @param width  int
     */
    public TerminalSize(int height, int width) {
        setHeight(height);
        setWidth(width);
    }

    /**
     * Gets the width of current and not readjusted terminal screen.
     *
     * @return int
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the width of terminal screen.<br>
     * This method throw an IllegalArgumentException in case width < 1.
     *
     * @param width int
     */
    public void setWidth(int width) {
        if (width < 1)
            throw new IllegalArgumentException("Terminal width cannot be less than 1");
        this.width = width;
    }

    /**
     * Gets the height of current and not readjusted terminal screen.
     *
     * @return int
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the height of terminal screen.<br>
     * This method throw an IllegalArgumentException in case height < 1.
     *
     * @param height int
     */
    public void setHeight(int height) {
        if (height < 1)
            throw new IllegalArgumentException("Terminal height cannot be less than 1");
        this.height = height;
    }

    /**
     * Gets the center width of current and not readjusted terminal screen.
     *
     * @return int
     */
    public int getCenterWidth() {
        return getWidth() / 2;
    }

    /**
     * Gets the center height of current and not readjusted terminal screen.
     *
     * @return int
     */
    public int getCenterHeight() {
        return getHeight() / 2;
    }

    public boolean isPositionWithinSize(CursorPosition pos) {
        return (pos.getRow() > -1 && pos.getColumn() > -1 &&
                pos.getRow() <= height && pos.getColumn() <= width);
    }

    @Override
    public String toString() {
        return "TerminalSize{" +
                "height=" + height +
                ", width=" + width +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalSize)) return false;

        TerminalSize that = (TerminalSize) o;

        return height == that.height && width == that.width;
    }

    @Override
    public int hashCode() {
        int result = height;
        result = 31 * result + width;
        return result;
    }
}
