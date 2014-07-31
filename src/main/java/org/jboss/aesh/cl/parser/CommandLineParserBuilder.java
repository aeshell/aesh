/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl.parser;

import org.jboss.aesh.cl.internal.ProcessedCommand;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineParserBuilder {

    private ProcessedCommand param;
    private boolean isChild = false;

    public CommandLineParserBuilder() {
    }

    public CommandLineParserBuilder child(boolean isChild) {
        this.isChild = isChild;
        return this;
    }

    public CommandLineParserBuilder(ProcessedCommand param) {
        this.param = param;
    }

    public CommandLineParserBuilder parameter(ProcessedCommand param) {
        this.param = param;
        return this;
    }

    public CommandLineParser generateParser() throws IllegalArgumentException {
        return new AeshCommandLineParser( param, isChild);
    }

}
