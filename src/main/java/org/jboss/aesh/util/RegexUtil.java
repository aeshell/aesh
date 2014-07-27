/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import java.util.regex.Pattern;

/**
 * A centralizer of all aesh regex Patterns.
 *
 * @author <a href="mailto:00hf11@gmail.com">Helio Frota</a>
 */
public enum RegexUtil {

    INSTANCE;

    public final Pattern aliasPattern = Pattern.compile("^(alias)\\s+(\\w+)\\s*=\\s*(.*)$");
    public final Pattern listAliasPattern = Pattern.compile("^(alias)((\\s+\\w+)+)$");
    public final Pattern exportPattern = Pattern.compile("^(export)\\s+(\\w+)\\s*=\\s*(\\S+).*$");
    public final Pattern variableDollarFirstPattern = Pattern.compile("\\$(\\w+|\\{(\\w+)\\})(.*)");
    public final Pattern variablePattern = Pattern.compile("(.*)\\$(\\w+|\\{(\\w+)\\})(.*)");
    public final Pattern boldRegex = Pattern.compile("(\\*[^']+\\*)|(\'\\S+\')|(::$)");
    public final Pattern controlOperatorPattern = Pattern.compile("(2>&1)|(2>>)|(2>)|(>>)|(>)|(<)|(\\|&)|(\\|\\|)|(\\|)|(;)|(&&)|(&)|(\")|(\')");
    public final Pattern redirectionNoPipelinePattern = Pattern.compile("(2>&1)|(2>>)|(2>)|(>>)|(>)|(<)");
    public final Pattern pipelineAndEndPattern = Pattern.compile("(\\|&)|(\\|)|(;)");
    public final Pattern variableSettingsPattern = Pattern.compile("^set\\s+(\\S+)\\s+(\\S+)$");
    public final Pattern commentPattern = Pattern.compile("^#.*");
    public final Pattern keyQuoteNamePattern = Pattern.compile("(^\"\\\\\\S+)(\":\\s+)(\\S+)");
    public final Pattern keyNamePattern = Pattern.compile("(^\\S+)(:\\s+)(\\S+)");
    public final Pattern keySeqPattern = Pattern.compile("^\"keyseq:\\s+\\b");
    public final Pattern startConstructs = Pattern.compile("^\\$if");
    public final Pattern endConstructs = Pattern.compile("^\\$endif");
    public final Pattern keyOperationPattern = Pattern.compile("(^\\\"\\\\M-\\[D:)(\\s+)(\\S+)");
    public final Pattern quotePattern = Pattern.compile("^\"");
    public final Pattern metaPattern = Pattern.compile("^(\\\\M|M|Meta)-"); // "M-
    public final Pattern controlPattern = Pattern.compile("^(\\\\C|C|Control)-"); // "M-
    public final Pattern starPattern = Pattern.compile("[\\*]+");
    public final Pattern spaceEscapedPattern = Pattern.compile("\\\\ ");
    public final Pattern spacePattern = Pattern.compile("(?<!\\\\)\\s");
    public final Pattern ansiPattern = Pattern.compile("\\u001B\\[[\\?]?[0-9;]*[a-zA-Z]?");
    // already commented line just moved to here.
    // public final Pattern keyNamePattern = Pattern.compile("^\\b:\\s+\\b");

}
