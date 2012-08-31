/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.console.operator;

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
    NONE;

    public static boolean isRedirectionOut(ControlOperator r) {
        return (r == PIPE || r == PIPE_OUT_AND_ERR || r == OVERWRITE_OUT
                || r == OVERWRITE_OUT_AND_ERR || r == APPEND_OUT);
    }

    public static boolean isRedirectionErr(ControlOperator r) {
        return (r == PIPE_OUT_AND_ERR || r == OVERWRITE_ERR
                || r == OVERWRITE_OUT_AND_ERR || r == APPEND_ERR);
    }
}
