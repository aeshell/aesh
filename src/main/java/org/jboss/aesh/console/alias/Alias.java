/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.alias;

/**
 * Alias value object
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class Alias implements Comparable {

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
        return (o instanceof Alias && ((Alias) o).getName().equals(getName()));
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

    @Override
    public int compareTo(Object o) {
        return getName().compareTo(((Alias )o ).getName());
    }
}
