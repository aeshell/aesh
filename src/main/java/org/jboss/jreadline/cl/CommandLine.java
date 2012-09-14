/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.cl;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLine {

    private List<ParsedOption> options;
    private List<String> arguments;

    public CommandLine() {
        options = new ArrayList<ParsedOption>();
        arguments = new ArrayList<String>();
    }

    public void addOption(ParsedOption option) {
        options.add(option);
    }

    public List<ParsedOption> getOptions() {
        return options;
    }

    public void addArgument(String arg) {
        arguments.add(arg);
    }

    public List<String> getArguments() {
        return arguments;
    }
}

