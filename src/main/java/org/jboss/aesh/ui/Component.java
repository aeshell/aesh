package org.jboss.aesh.ui;

import org.jboss.aesh.terminal.TerminalColor;

import java.util.List;

public class Component {

    private Rectangle rectangle;

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


}
