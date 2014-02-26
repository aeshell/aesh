/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.operator;

/**
 * A token that performs a control function.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public enum ControlOperator {
    PIPE, // |
    PIPE_OUT_AND_ERR, // |&
    OVERWRITE_OUT, // >
    APPEND_OUT, // >>
    OVERWRITE_IN, // <
    OVERWRITE_ERR, // 2>
    APPEND_ERR, // 2>>
    OVERWRITE_OUT_AND_ERR, // 2>&1
    END, // ;
    AMP, // &
    AND, // &&
    OR, // ||
    NONE;

    public boolean isRedirectionOut() {
        return (this == PIPE || this == PIPE_OUT_AND_ERR || this == OVERWRITE_OUT
                || this == OVERWRITE_OUT_AND_ERR || this == APPEND_OUT);
    }

    public boolean isRedirectionErr() {
        return (this == PIPE_OUT_AND_ERR || this == OVERWRITE_ERR
                || this == OVERWRITE_OUT_AND_ERR || this == APPEND_ERR);
    }

    public boolean isRedirect() {
        return !(this == END || this == AMP || this == AND || this == NONE || this == OVERWRITE_IN);
    }

    public boolean isPipe() {
        return this == PIPE || this == PIPE_OUT_AND_ERR;
    }

    public boolean isOut() {
        return this == APPEND_OUT || this == OVERWRITE_OUT || this == OVERWRITE_OUT_AND_ERR;
    }

    public boolean isErr() {
        return this == APPEND_ERR || this == OVERWRITE_ERR || this == OVERWRITE_OUT_AND_ERR;
    }

    private boolean isNone() {
        return this == NONE;
    }
}
