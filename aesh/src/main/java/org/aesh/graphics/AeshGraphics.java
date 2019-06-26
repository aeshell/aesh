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
package org.aesh.graphics;

import org.aesh.readline.terminal.formatting.TerminalColor;
import org.aesh.readline.terminal.formatting.TerminalTextStyle;
import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Capability;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.utils.ANSI;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class AeshGraphics implements Graphics {

    private static final String CURSOR_DOWN = ANSI.START+"1B"+ANSI.START+"1D";

    private final Connection connection;
    private final GraphicsConfiguration graphicsConfiguration;
    private TerminalColor currentColor;
    private TerminalTextStyle currentStyle;


    AeshGraphics(Connection connection, GraphicsConfiguration graphicsConfiguration) {
        this.connection = connection;
        this.graphicsConfiguration = graphicsConfiguration;
        currentColor = new TerminalColor();
        connection.put(Capability.cursor_invisible);
    }

    @Override
    public void flush() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        printColor(new TerminalColor());
        connection.put(Capability.clear_screen);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearAndShowCursor() {
        clear();
        connection.put(Capability.cursor_normal);
    }

    @Override
    public TerminalColor getColor() {
        return currentColor;
    }

    @Override
    public void setColor(TerminalColor color) {
        this.currentColor = color;
    }

    @Override
    public TerminalTextStyle getTextStyle() {
        return currentStyle;
    }

    @Override
    public void setTextStyle(TerminalTextStyle textStyle) {
        this.currentStyle = textStyle;
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        printColor(currentColor);
        drawHorizontalLine(x, y, width);
        drawHorizontalLine(x,y+height,width);
        drawVerticalLine(x, y+1, height-1);
        drawVerticalLine(x+width-1,y+1,height-1);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        printColor(currentColor);
        int dx = x2 - x1;
        int dy = y2 -y1;
        for(int i=x1; i < x2; i++) {
            int y = y1 + (dy) * (i - x1)/(dx);
            connection.put(Capability.cursor_address, y, i);
            connection.write("x");
        }
    }

    @Override
    public void drawString(String str, int x, int y) {
        printColor(currentColor);
        connection.put(Capability.cursor_address, y, x);
        connection.write(str);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        printColor(currentColor);
        for(int j=0; j < height; j++) {
            connection.put(Capability.cursor_address, y + j, x);
            for(int i=0; i < width; i++)
                connection.write(" ");
        }
    }

    @Override
    public void drawCircle(int x0, int y0, int radius) {
        printColor(currentColor);
        int x = radius, y = 0;
        int radiusError = 1-x;

        while(x >= y) {
            drawPixel(x + x0, y + y0);
            drawPixel(y + x0, x + y0);
            drawPixel(-x + x0, y + y0);
            drawPixel(-y + x0, x + y0);
            drawPixel(-x + x0, -y + y0);
            drawPixel(-y + x0, -x + y0);
            drawPixel(x + x0, -y + y0);
            drawPixel(y + x0, -x + y0);

            y++;
            if(radiusError<0)
                radiusError+=2*y+1;
            else {
                x--;
                radiusError+=2*(y-x+1);
            }
        }
    }

    private void printColor(TerminalColor color) {
        if(color != null)
            connection.write(color.fullString());
    }

    private void drawPixel(int x, int y) {
        connection.put(Capability.cursor_address, y, x);
        connection.write("x");
    }

    private void drawHorizontalLine(int x, int y, int width) {
        Size terminalSize = graphicsConfiguration.getBounds();
        if(terminalSize.getHeight() > y && terminalSize.getWidth() > y) {
            if(terminalSize.getWidth() < x + width)
                width = terminalSize.getWidth() - x-1;
            connection.put(Capability.cursor_address, y, x);
            char[] line = new char[width];
            for(int i=0; i < line.length; i++) {
                line[i] = (i == 0 || i == line.length - 1) ? 'x' : '-';
            }
            connection.write(new String(line));
        }
    }

    private void drawVerticalLine(int x, int y, int length) {
        Size terminalSize = graphicsConfiguration.getBounds();
        if(terminalSize.getHeight() > y && terminalSize.getWidth() > y) {
            if(terminalSize.getHeight() < y + length)
                length = terminalSize.getHeight() - y-1;
            connection.put(Capability.cursor_address, y, x);
            for(int i=0; i < length; i++) {
                connection.write("|");
                connection.write(CURSOR_DOWN);
            }
        }
    }

}
