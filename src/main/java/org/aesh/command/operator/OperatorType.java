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
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.operator;

/**
 * Operators. Only configuration operators can have an argument.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public enum OperatorType {
    PIPE("|"),
    REDIRECT_OUT(">", true),
    REDIRECT_IN("<", true),
    END(";"),
    APPEND_OUT(">>", true),
    NONE("");

    private final String value;
    private final boolean hasArgument;
    private final boolean isConfiguration;

    OperatorType(String c, boolean hasArgument) {
        value = c;
        this.hasArgument = hasArgument;
        this.isConfiguration = true;
    }

    OperatorType(String c) {
        value = c;
        this.hasArgument = false;
        this.isConfiguration = false;
    }

    public String value() {
        return value;
    }

    public boolean hasArgument() {
        return hasArgument;
    }

    public boolean isConfiguration() {
        return isConfiguration;
    }
}
