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

import java.util.Set;

/**
 * Operators. Only configuration operators can have an argument.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public enum OperatorType {
    PIPE("|", false, true),
    PIPE_AND_ERROR("|&"),
    REDIRECT_OUT(">", true, true),
    REDIRECT_OUT_ERROR("2>", true, true),
    REDIRECT_IN("<", true, true),
    END(";"),
    APPEND_OUT(">>", true, true),
    APPEND_OUT_ERROR("2>>", true, true),
    REDIRECT_OUT_ALL("2>&1", true, true),
    AMP("&", true, true),
    AND("&&"),
    OR("||"),
    NONE("");

    private final String value;
    private final boolean hasArgument;
    private final boolean isConfiguration;

    OperatorType(String c, boolean hasArgument, boolean isConfiguration) {
        value = c;
        this.hasArgument = hasArgument;
        this.isConfiguration = isConfiguration;
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

    public boolean matches(String text, int index) {
        if(text.length() >= index+value.length()) {
            for(int i=0; i < value.length(); i++)
                if(text.charAt(index+i) != value.charAt(i))
                    return false;

            return true;
        }
        return false;
    }

    public static boolean isAppendOrRedirectInOrOut(OperatorType type) {
        return type == APPEND_OUT || type == REDIRECT_OUT || type == REDIRECT_IN ||
                type == REDIRECT_OUT_ALL || type == REDIRECT_OUT_ERROR ||
                type == APPEND_OUT_ERROR;
    }

    public static OperatorType matches(Set<OperatorType> operators, String text, int index) {
        OperatorType found = OperatorType.NONE;
        for(OperatorType operator : operators) {
            if(operator.matches(text, index)) {
                if(found.value().length() < operator.value().length())
                    found = operator;
            }
        }
        return found;
    }
}
