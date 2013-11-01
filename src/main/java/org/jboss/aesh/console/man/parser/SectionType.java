/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.man.parser;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public enum SectionType {
    NAME("NAME"),
    SYNOPSIS("SYNOPSIS"),
    DESCRIPTION("DESCRIPTION"),
    OPTIONS("OPTIONS"),
    AUTHOR("AUTHOR"),
    RESOURCES("RESOURCES"),
    COPYING("COPYING"),
    NOT_SPECIFIED("NOT_SPECIFIED"),
    IGNORED("IGNORED");

    private String type;

    SectionType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static SectionType getSectionType(String name) {
        if(NAME.getType().equals(name))
            return NAME;
        else if(SYNOPSIS.getType().equals(name))
            return SYNOPSIS;
        else if(DESCRIPTION.getType().equals(name))
            return DESCRIPTION;
        else if(OPTIONS.getType().equals(name))
            return OPTIONS;
        else if(AUTHOR.getType().equals(name))
            return AUTHOR;
        else if(RESOURCES.getType().equals(name))
            return RESOURCES;
        else if(COPYING.getType().equals(name))
            return COPYING;
        else
            return NOT_SPECIFIED;
    }

}
