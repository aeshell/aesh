/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.internal.ProcessedCommand;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParserBuilder {

    private ProcessedCommand param;

    public ParserBuilder() {
    }

    public ParserBuilder(ProcessedCommand param) {
        this.param = param;
    }

    public ParserBuilder parameter(ProcessedCommand param) {
        this.param = param;
        return this;
    }

    public CommandLineParser generateParser() throws IllegalArgumentException {
        return new CommandLineParser( param);
    }

}
