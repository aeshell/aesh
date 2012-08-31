/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.console.alias;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Alias {

    private String name;
    private String value;

    public Alias(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Alias) {
            return ((Alias) o).getName().equals(getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 9320012;
    }

    @Override
    public String toString() {
        return new StringBuilder(getName()).append("='")
                .append(getValue()).append("'").toString();
    }
}
