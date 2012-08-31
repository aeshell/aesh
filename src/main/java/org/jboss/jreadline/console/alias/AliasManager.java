/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
