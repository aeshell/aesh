/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
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
