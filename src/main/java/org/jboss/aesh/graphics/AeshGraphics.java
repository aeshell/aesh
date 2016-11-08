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

import org.aesh.terminal.Terminal;
import org.aesh.terminal.formatting.TerminalColor;
import org.aesh.terminal.formatting.TerminalTextStyle;
import org.aesh.tty.Capability;
import org.aesh.tty.Size;
import org.aesh.util.ANSI;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class AeshGraphics implements Graphics {

    private static final String CURSOR_DOWN = ANSI.START+"1B"+ANSI.START+"1D";

    private final Terminal terminal;
    private final GraphicsConfiguration graphicsConfiguration;
    private TerminalColor currentColor;
    private TerminalTextStyle currentStyle;


    AeshGraphics(Terminal terminal, GraphicsConfiguration graphicsConfiguration) {
        this.terminal = terminal;
        this.graphicsConfiguration = graphicsConfiguration;
        currentColor = new TerminalColor();
        terminal.puts(Capability.cursor_invisible);
    }

    @Override
    public void flush() {
        terminal.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        printColor(new TerminalColor());
        terminal.puts(Capability.clear_screen);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearAndShowCursor() {
        clear();
        terminal.puts(Capability.cursor_normal);
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
            terminal.puts(Capability.cursor_address, y, i);
            terminal.writer().write('x');
        }
    }

    @Override
    public void drawString(String str, int x, int y) {
        printColor(currentColor);
        terminal.puts(Capability.cursor_address, y, x);
        terminal.writer().write(str);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        printColor(currentColor);
        for(int j=0; j < height; j++) {
            terminal.puts(Capability.cursor_address, y + j, x);
            for(int i=0; i < width; i++)
                terminal.writer().write(' ');
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
            terminal.writer().write(color.fullString());
    }

    private void drawPixel(int x, int y) {
        terminal.puts(Capability.cursor_address, y, x);
        terminal.writer().write('x');
    }

    private void drawHorizontalLine(int x, int y, int width) {
        Size terminalSize = graphicsConfiguration.getBounds();
        if(terminalSize.getHeight() > y && terminalSize.getWidth() > y) {
            if(terminalSize.getWidth() < x + width)
                width = terminalSize.getWidth() - x-1;
            terminal.puts(Capability.cursor_address, y, x);
            char[] line = new char[width];
            for(int i=0; i < line.length; i++) {
                line[i] = (i == 0 || i == line.length - 1) ? 'x' : '-';
            }
            terminal.writer().write(line);
        }
    }

    private void drawVerticalLine(int x, int y, int length) {
        Size terminalSize = graphicsConfiguration.getBounds();
        if(terminalSize.getHeight() > y && terminalSize.getWidth() > y) {
            if(terminalSize.getHeight() < y + length)
                length = terminalSize.getHeight() - y-1;
            terminal.puts(Capability.cursor_address, y, x);
            for(int i=0; i < length; i++) {
                terminal.writer().write('|');
                terminal.writer().write(CURSOR_DOWN);
            }
        }
    }

}
