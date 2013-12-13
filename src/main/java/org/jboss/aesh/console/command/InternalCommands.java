/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.command;

/**
 * List over internal commands/functions in aesh
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public enum InternalCommands {
    ALIAS("alias"),
    UNALIAS("unalias"),
    EXPORT("export"),
    ECHO("echo");

    private String command;
    InternalCommands(String alias) {
        this.command = alias;
    }

    public String getCommand() {
        return command;
    }
}
