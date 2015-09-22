/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.graphics;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class RootPane extends Component {

    private String title;
    private List<Component> components;

    protected RootPane(String title, Graphics g) {
        setGraphics(g);
        setTitle(title);
        components = new ArrayList<>();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void addComponent(Component c) {
        components.add(c);
    }

    @Override
    public void paint() {
        //print title
        paintBorder();
        getGraphics().drawString(title, getBounds().getCenterPointX()-(title.length()/2), 2);

        for(Component c : components)
            c.paint();
    }
}
