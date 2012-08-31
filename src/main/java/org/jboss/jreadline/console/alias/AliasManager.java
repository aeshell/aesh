/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.console.alias;

import org.jboss.jreadline.console.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AliasManager {

    private List<Alias> aliases;

    public AliasManager(File aliasFile) {
        aliases = new ArrayList<Alias>();
        addAlias("test", "testing");
        addAlias("bar", "barbar");
    }

    public void addAlias(String name, String value) {
        Alias alias = new Alias(name, value);
        if(aliases.contains(alias)) {
            aliases.remove(alias);
        }
        aliases.add(alias);
    }

    public String showAll() {
        StringBuilder sb = new StringBuilder();
        for(Alias a : aliases)
            sb.append("alias ").append(a.toString()).append(Config.getLineSeparator());

        return sb.toString();
    }

    public Alias getAlias(String name) {
        int index = aliases.indexOf(new Alias(name, ""));
        if(index > 0)
            return aliases.get(index);
        else
            return null;
    }

    public List<String> findAllMatchingNames(String name) {
        List<String> names = new ArrayList<String>();
        for(Alias a : aliases)
            if(a.getName().startsWith(name))
                names.add(a.getName());

        return names;
    }

    public List<String> getAllNames() {
        List<String> names = new ArrayList<String>();
        for(Alias a : aliases)
            names.add(a.getName());

        return names;
    }
}
