/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
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
