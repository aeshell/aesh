package org.jboss.aesh.graphics;

import org.jboss.aesh.terminal.Color;
import org.jboss.aesh.terminal.TerminalTextStyle;
import org.jboss.aesh.ui.Rectangle;

/**
 * Created by stalep on 7/5/15.
 */
public abstract class Component {

    private Graphics graphics;

    public Graphics getGraphics() {
        return graphics;
    }

    public void setGraphics(Graphics g) {
        graphics = g;
    }

    public void setBounds(Rectangle bounds) {
        graphics.setBounds(bounds);
    }

    public Rectangle getBounds() {
        return graphics.getBounds();
    }

    public Color getTextColor() {
        return graphics.getColor().getTextColor();
    }

    public Color getBackGroundColor() {
        return graphics.getColor().getBackgroundColor();
    }

    public TerminalTextStyle getTextStyle() {
        return graphics.getTextStyle();
    }
}
