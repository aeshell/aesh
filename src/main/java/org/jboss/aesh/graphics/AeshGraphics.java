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

import org.jboss.aesh.terminal.Color;
import org.jboss.aesh.terminal.CursorPosition;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.terminal.TerminalColor;
import org.jboss.aesh.terminal.TerminalTextStyle;
import org.jboss.aesh.util.ANSI;
import org.jboss.aesh.util.LoggerUtil;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshGraphics implements Graphics {

    private static final String CURSOR_DOWN = ANSI.START+"1B"+ANSI.START+"1D";

    private final Shell shell;
    private TerminalColor currentColor;
    private TerminalTextStyle currentStyle;
    private Rectangle bounds;
    private Point translatedPoint;
    private final static Logger LOGGER = LoggerUtil.getLogger(AeshGraphics.class.getName());

    public AeshGraphics(Shell shell) {
        this.shell = shell;
        currentColor = new TerminalColor();
        shell.out().print(ANSI.CURSOR_HIDE);
        bounds = new Rectangle(0, 0, shell.getSize().getWidth(), shell.getSize().getHeight());
        translatedPoint = new Point(0,0);
    }

    @Override
    public void flush() {
        shell.out().flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        try {
            shell.out().println(new TerminalColor(Color.DEFAULT, Color.DEFAULT).fullString());
            shell.clear();
        }
        catch (IOException ignored) { }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearAndShowCursor() {
        clear();
        shell.out().print(ANSI.CURSOR_SHOW);
        shell.out().flush();
    }

    @Override
    public TerminalColor getColor() {
        return currentColor;
    }

    @Override
    public void setBounds(Rectangle rectangle) {
        this.bounds = new Rectangle(rectangle);
    }

    @Override
    public Rectangle getBounds() {
        return bounds;
    }

    @Override
    public void translate(int x, int y) {

        LOGGER.info("bounds before translate: "+bounds);

        int width = bounds.getWidth();
        if(width + x > shell.getSize().getWidth())
            width = shell.getSize().getWidth()-x;
        int height = bounds.getHeight();
        if(height + y > shell.getSize().getHeight())
            height = shell.getSize().getHeight()-y;

        bounds = new Rectangle(0, 0, width, height);
        translatedPoint = new Point(x,y);
        LOGGER.info("translating to: "+bounds);
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
        boolean insideStartBounds = bounds.isInside(x1, y1);
        boolean insideEndBounds = bounds.isInside(x2,y2);
        if(!insideStartBounds && !insideEndBounds)
            return;
        if(!insideEndBounds || !insideStartBounds)
            insideEndBounds = false;
        else
            insideEndBounds = true;

        int dx = x2 - x1;
        int dy = y2 -y1;
        int y = 0;
        for(int i=x1; i < x2; i++) {
            y = y1 + (dy) * (i - x1)/(dx);
            if(insideEndBounds) {
                shell.setCursor(new CursorPosition(y, i));
                shell.out().print('x');
            }
            else {
                if(bounds.isInside(i, y)) {
                    shell.setCursor(new CursorPosition(y, i));
                    shell.out().print('x');
                }
            }
        }
    }

    @Override
    public void drawString(String str, int x, int y) {
        if(bounds.isInside(x, y)) {
            if (currentColor != null)
                shell.out().print(currentColor.fullString());
            if(bounds.isInside(x+str.length(), y)) {
                shell.setCursor(new CursorPosition(y, x));
                shell.out().print(str);
            }
            else {
                int length = (x+str.length() - (bounds.getX() + bounds.getWidth()));
                shell.setCursor(new CursorPosition(y, x));
                shell.out().print(str.substring(0,length));
            }
        }
        else if(bounds.isInside(x+str.length(), y)) {
            int rest = x+str.length()-bounds.getX();
            drawString(str.substring(rest), x + rest, y);
        }
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        //the whole rect is inside bounds
        if(bounds.isInside(x, y) && bounds.isInside(x+width, y+height)) {
            LOGGER.info("IS INSIDE");
            doFillRect(x, y, width, height);
        }
        //start point is inside bounds, but not end point
        else if(bounds.isInside(x, y)) {
            int newWidth = width;
            int newHeight = height;
            //x axis is within bounds
            if(!bounds.isInside(x+width, y)) {
                //
               newWidth =- (x+width - (bounds.getX() + bounds.getWidth()));
            }
            if(!bounds.isInside(x, y+height))
                newHeight = - (y+height - (bounds.getY() + bounds.getHeight()));

            doFillRect(x, y, newWidth, newHeight);
        }
        //end points are inside bounds, but not start points
        else {
            if(bounds.isInside(bounds.getX(), y)) {
                width = width - (bounds.getX() - x);
                x = bounds.getX();
            }
            if(bounds.isInside(x, bounds.getY())) {
                height = height - (bounds.getY() - y);
                y = bounds.getY();
            }
            doFillRect(x, y, width, height);
        }
    }

    private void doFillRect(int x, int y, int width, int height) {
        if (currentColor != null)
            shell.out().print(currentColor.fullString());

        x += translatedPoint.getX();
        y += translatedPoint.getY();
        for (int j = 0; j < height; j++) {
            shell.setCursor(new CursorPosition(y + j, x));
            for (int i = 0; i < width; i++)
                shell.out().print(' ');
        }
    }

    @Override
    public void drawCircle(int x0, int y0, int radius) {
        if(currentColor != null)
            shell.out().print(currentColor.fullString());
        int x = radius, y = 0;
        int radiusError = 1-x;

        x0 += translatedPoint.getX();
        y0 += translatedPoint.getY();

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
        if(bounds.isInside(x, y)) {
            shell.setCursor(new CursorPosition(y+translatedPoint.getY(), x+translatedPoint.getX()));
            shell.out().print('x');
        }
    }


    private void drawHorizontalLine(int x, int y, int width) {
        //check that we start in bounds
        if(x < bounds.getX()) {
            //if x + width < x, just return
            if((x+width) < bounds.getX())
                return;
            width =- (bounds.getX()-x);
            x = bounds.getX();
        }
        //if x is outside of our boundaries, just return
        if(x > bounds.getX() + bounds.getWidth())
            return;
        // if y is outside our boundaries, just return
        if(y < bounds.getY() || y > (bounds.getY()+bounds.getHeight()))
            return;

        //if(bounds.getHeight() > y && bounds.getWidth() > x) {
        //    if(bounds.getWidth() < x + width)
        //        width = bounds.getWidth() - x-1;
        shell.setCursor( new CursorPosition(
                y+translatedPoint.getY(),
                x+translatedPoint.getX()));
        char[] line = new char[width];
        for(int i=0; i < line.length; i++) {
            if(i == 0 || i == line.length-1)
                line[i] = 'x';
            else
                line[i] = '-';
        }
        shell.out().print(line);
    }

    private void drawVerticalLine(int x, int y, int length) {

        if(bounds.getHeight() > y && bounds.getWidth() > x) {
            if(bounds.getHeight() < y + length)
                length = bounds.getHeight() - y-1;
            shell.setCursor(new CursorPosition(y+translatedPoint.getY(),x+translatedPoint.getX()));
            for(int i=0; i < length; i++) {
                shell.out().print('|');
                shell.out().print(CURSOR_DOWN);
            }
        }
    }
}
