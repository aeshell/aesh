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
package org.jboss.aesh.graphics;

import org.jboss.aesh.terminal.TerminalColor;
import org.jboss.aesh.terminal.TerminalTextStyle;

/**
 * Simple Terminal Graphics API
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface Graphics {

    /**
     * Flush the graphics to the terminal.
     * No action will happen on the terminal unless flush is called.
     */
    void flush();

    /**
     * Clear the entire terminal screen
     */
    void clear();

    /**
     * Reset current terminal color to default and enable visible cursor
     */
    void cleanup();

    TerminalColor getColor();

    /**
     * Sets this graphics context's current color to the specified color.
     *
     * @param color terminalColor
     */
    void setColor(TerminalColor color);

    TerminalTextStyle getTextStyle();

    /**
     * Set this graphics context's current text style
     * Note: Not implemented yet
     *
     * @param textStyle stype
     */
    void setTextStyle(TerminalTextStyle textStyle);

    /**
     * Draws the outline of the specified rectangle.
     * The left and right edges of the rectangle are at x and x + width.
     * The top and bottom edges are at y and y + height.
     * The rectangle is drawn using the graphics context's current color.
     *
     * @param x
     * @param y
     * @param width
     * @param height
     */
    void drawRect(int x, int y, int width, int height);

    /**
     * Draws a line, using the current color,
     * between the points (x1, y1) and (x2, y2) in this graphics context's coordinate system.
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     */
    void drawLine(int x1, int y1, int x2, int y2);

    /**
     * Draws the text given by the specified string,
     * using this graphics context's current font and color.
     * The baseline of the leftmost character is at position (x, y) in this graphics context's coordinate system.
     *
     * @param str
     * @param x
     * @param y
     */
    void drawString(String str, int x, int y);

    /**
     * Fills the specified rectangle. The left and right edges of the rectangle are at x and x + width - 1.
     * The top and bottom edges are at y and y + height - 1.
     * The resulting rectangle covers an area width pixels wide by height pixels tall.
     * The rectangle is filled using the graphics context's current color.
     *
     * @param x
     * @param y
     * @param width
     * @param height
     */
    void fillRect(int x, int y, int width, int height);


    /**
     * Draw a Circle using the given x,y as center
     * Note: the circle is more like an oval atm..
     * @param x
     * @param y
     * @param radius
     */
    void drawCircle(int x, int y, int radius);

}
