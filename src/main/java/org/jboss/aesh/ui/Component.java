package org.jboss.aesh.ui;

import org.jboss.aesh.terminal.TerminalColor;

import java.util.ArrayList;
import java.util.List;

public class Component {

    private Rectangle bounds;

    private TerminalColor color;

    private boolean ignoreRepaint = false;

    private Component parent;

    private List<Component> components;
    /**
     * The components name
     */
    private String name;

    /**
     * If this component can be selectable
     */
    private boolean selectable;


    public Component() {
        components = new ArrayList<>();
    }


    public Rectangle getBounds() {
        return bounds;
    }

    public void setBounds(Rectangle rectangle) {
        bounds = rectangle.getBounds();
    }

    public void setParent(Component parent) {
        this.parent = parent;
    }

    public void add(Component component) {
        component.setParent(this);
        components.add(component);

    }
}
