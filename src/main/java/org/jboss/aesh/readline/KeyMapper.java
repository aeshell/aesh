/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.readline;

import org.jboss.aesh.terminal.InfocmpManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class KeyMapper {

    private List<ActionEvent> events;

    public KeyMapper() {
        events = new ArrayList<>();

        readFromInputcmp();
    }

    private void readFromInputcmp() {
        events.add(new ActionEvent("delete", InfocmpManager.getDelete()));
        events.add(new ActionEvent("insert", InfocmpManager.getIns()));
        events.add(new ActionEvent("up", InfocmpManager.getUp()));
        events.add(new ActionEvent("down", InfocmpManager.getDown()));
        events.add(new ActionEvent("left", InfocmpManager.getLeft()));
        events.add(new ActionEvent("right", InfocmpManager.getRight()));
    }

    public ActionEvent getByName(String name) {
        return events.stream().filter( x -> x.name().equals(name)).findFirst().get();
    }

    public List<ActionEvent> getEvents() {
        return events;
    }

}
