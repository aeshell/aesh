/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.alias;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.util.LoggerUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages Aliases
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AliasManager {

    private List<Alias> aliases;
    private Pattern aliasPattern = Pattern.compile("^(alias)\\s+(\\w+)\\s*=\\s*(.*)$");
    private Pattern listAliasPattern = Pattern.compile("^(alias)((\\s+\\w+)+)$");
    private static final String ALIAS = "alias";
    private static final String ALIAS_SPACE = "alias ";
    private static final String UNALIAS = "unalias";
    private File aliasFile;
    private String name;
    private boolean persistAlias = false;
    private static final Logger LOGGER = LoggerUtil.getLogger(AliasManager.class.getName());

    public AliasManager(File aliasFile, boolean persistAlias, String name) throws IOException {
        this.persistAlias = persistAlias;
        this.name = name;
        aliases = new ArrayList<>();
        if(aliasFile != null) {
            this.aliasFile = aliasFile;
            if(this.aliasFile.isFile())
                readAliasesFromFile();
        }
    }

    private void readAliasesFromFile() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(aliasFile))) {
            String line;
            while((line = br.readLine()) != null) {
                if(line.startsWith(ALIAS)) {
                    try {
                        parseAlias(line);
                    }
                    catch (Exception ignored) {
                    }
                }
            }
        }
    }

    public void persist() throws IOException {
        if(persistAlias && aliasFile != null) {
            //just do it easily and remove the current file
            if(aliasFile.isFile())
                aliasFile.delete();

            try (FileWriter fw = new FileWriter(aliasFile)) {
                LOGGER.info("created fileWriter");
                Collections.sort(aliases); // not very efficient, but it'll do for now...
                for(Alias a : aliases) {
                    LOGGER.info("writing to file: "+ALIAS_SPACE+a.toString());
                    fw.write(ALIAS_SPACE+a.toString()+Config.getLineSeparator());
                }
                fw.flush();
            }
        }
    }

    public void addAlias(String name, String value) {
        Alias alias = new Alias(name, value);
        if(aliases.contains(alias)) {
            aliases.remove(alias);
        }
        aliases.add(alias);
    }

    @SuppressWarnings("unchecked")
    public String printAllAliases() {
        StringBuilder sb = new StringBuilder();
        Collections.sort(aliases); // not very efficient, but it'll do for now...
        for(Alias a : aliases)
            sb.append(ALIAS_SPACE).append(a.toString()).append(Config.getLineSeparator());

        return sb.toString();
    }

    public Alias getAlias(String name) {
        int index = aliases.indexOf(new Alias(name, null));
        if(index > -1)
            return aliases.get(index);
        else
            return null;
    }

    public List<String> findAllMatchingNames(String name) {
        List<String> names = new ArrayList<>();
        for(Alias a : aliases)
            if(a.getName().startsWith(name))
                names.add(a.getName());

        return names;
    }

    public List<String> getAllNames() {
        List<String> names = new ArrayList<>();
        for(Alias a : aliases)
            names.add(a.getName());

        return names;
    }

    public String removeAlias(String buffer) {
        if(buffer.trim().equals(UNALIAS))
            return "unalias: usage: unalias name [name ...]"+Config.getLineSeparator();

        buffer = buffer.substring(UNALIAS.length()).trim();
        for(String s : buffer.split(" ")) {
            if(s != null) {
                Alias a = getAlias(s.trim());
                if(a != null)
                    aliases.remove(a);
                else
                    return name+": unalias: "+s+": not found" +Config.getLineSeparator();
            }
        }
        return null;
    }

    public String parseAlias(String buffer) {
        if(buffer.trim().equals(ALIAS))
            return printAllAliases();
        Matcher aliasMatcher = aliasPattern.matcher(buffer);
        if(aliasMatcher.matches()) {
            String name = aliasMatcher.group(2);
            String value = aliasMatcher.group(3);
            if(value.startsWith("'")) {
                if(value.endsWith("'"))
                    value = value.substring(1,value.length()-1);
                else
                    return "alias: usage: alias [name[=value] ... ]"+Config.getLineSeparator();
            }
            else if(value.startsWith("\"")) {
                if(value.endsWith("\""))
                    value = value.substring(1,value.length()-1);
                else
                    return "alias: usage: alias [name[=value] ... ]"+Config.getLineSeparator();
            }
            if(name.contains(" "))
                return "alias: usage: alias [name[=value] ... ]"+Config.getLineSeparator();

            addAlias(name, value);
            return null;
        }

        Matcher listMatcher = listAliasPattern.matcher(buffer);
        if(listMatcher.matches()) {
            StringBuilder sb = new StringBuilder();
                for(String s : listMatcher.group(2).trim().split(" ")) {
                if(s != null) {
                    Alias a = getAlias(s.trim());
                    if(a != null)
                        sb.append(ALIAS_SPACE).append(a.getName()).append("='")
                                .append(a.getValue()).append("'").append(Config.getLineSeparator());
                    else
                        sb.append(name).append(": alias: ").append(s)
                                .append(" : not found").append(Config.getLineSeparator());
                }
            }
            return sb.toString();
        }
        return null;
    }

}
