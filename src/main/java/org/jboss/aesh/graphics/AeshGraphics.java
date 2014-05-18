/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.graphics;

import org.jboss.aesh.terminal.Color;
import org.jboss.aesh.terminal.CursorPosition;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.terminal.TerminalColor;
import org.jboss.aesh.terminal.TerminalSize;
import org.jboss.aesh.terminal.TerminalTextStyle;
import org.jboss.aesh.util.ANSI;

import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshGraphics implements Graphics {

    private static final String CURSOR_DOWN = ANSI.getStart()+"1B"+ANSI.getStart()+"1D";

    private Shell shell;
    private GraphicsConfiguration graphicsConfiguration;
    private TerminalColor currentColor;
    private TerminalTextStyle currentStyle;

    AeshGraphics(Shell shell, GraphicsConfiguration graphicsConfiguration) {
        this.shell = shell;
        this.graphicsConfiguration = graphicsConfiguration;
        currentColor = new TerminalColor();
        shell.out().print(ANSI.hideCursor());
    }

    @Override
    public void flush() {
        shell.out().flush();
    }

    @Override
    public void clear() {
        try {
            shell.out().println(new TerminalColor(Color.DEFAULT, Color.DEFAULT).fullString());
            shell.clear();
        }
        catch (IOException ignored) { }
    }

    @Override
    public void cleanup() {
        shell.out().print(new TerminalColor(Color.DEFAULT, Color.DEFAULT).fullString());
        shell.out().print(ANSI.showCursor());
        shell.out().flush();
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
        if(currentColor != null)
            shell.out().print(currentColor.fullString());
        drawHorizontalLine(x, y, width);
        drawHorizontalLine(x,y+height,width);
        drawVerticalLine(x, y+1, height-1);
        drawVerticalLine(x+width-1,y+1,height-1);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        if(currentColor != null)
            shell.out().print(currentColor.fullString());
        int dx = x2 - x1;
        int dy = y2 -y1;
        int y = 0;
        for(int i=x1; i < x2; i++) {
            y = y1 + (dy) * (i - x1)/(dx);
            shell.setCursor(new CursorPosition(y,i));
            shell.out().print('x');
        }
    }

    @Override
    public void drawString(String str, int x, int y) {
        if(currentColor != null)
            shell.out().print(currentColor.fullString());
        shell.setCursor(new CursorPosition(y,x));
        shell.out().print(str);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        if(currentColor != null)
            shell.out().print(currentColor.fullString());

        for(int j=0; j < height; j++) {
            shell.setCursor(new CursorPosition(y+j,x));
            for(int i=0; i < width; i++)
                shell.out().print(' ');
        }
    }

    @Override
    public void drawCircle(int x0, int y0, int radius) {
        if(currentColor != null)
            shell.out().print(currentColor.fullString());
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

    private void drawPixel(int x, int y) {
        shell.setCursor(new CursorPosition(y,x));
        shell.out().print('x');
    }


    private void drawHorizontalLine(int x, int y, int width) {
        TerminalSize terminalSize = graphicsConfiguration.getBounds();
        if(terminalSize.getHeight() > y && terminalSize.getWidth() > y) {
            if(terminalSize.getWidth() < x + width)
                width = terminalSize.getWidth() - x-1;
            shell.setCursor(new CursorPosition(y,x));
            char[] line = new char[width];
            for(int i=0; i < line.length; i++) {
                if(i == 0 || i == line.length-1)
                    line[i] = 'x';
                else
                    line[i] = '-';
            }
            shell.out().print(line);
        }
    }

    private void drawVerticalLine(int x, int y, int length) {
        TerminalSize terminalSize = graphicsConfiguration.getBounds();
        if(terminalSize.getHeight() > y && terminalSize.getWidth() > y) {
            if(terminalSize.getHeight() < y + length)
                length = terminalSize.getHeight() - y-1;
            shell.setCursor(new CursorPosition(y,x));
            for(int i=0; i < length; i++) {
                shell.out().print('|');
                shell.out().print(CURSOR_DOWN);
            }
        }
    }
}
