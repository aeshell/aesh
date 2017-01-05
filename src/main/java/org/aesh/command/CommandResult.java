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

package org.aesh.command;

/**
 * The result of a command execution
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class CommandResult {
    public static final CommandResult SUCCESS = new CommandResult(0);
    public static final CommandResult FAILURE = new CommandResult(-1);

    private final int result;

    /**
     * Converts the given result integer into a {@link CommandResult}
     *
     * @param result
     * @return {@link CommandResult#SUCCESS} if result == 0, {@link CommandResult#FAILURE} if result == -1 or a new instance if
     *         different from -1 and 0
     */
    public static CommandResult valueOf(final int result) {
        if (result == 0) {
            return SUCCESS;
        }
        else if (result == -1) {
            return FAILURE;
        }
        else {
            return new CommandResult(result);
        }
    }

    private CommandResult(int result) {
        this.result = result;
    }

    public int getResultValue() {
        return result;
    }

    public boolean isSuccess() {
        return getResultValue() == 0;
    }

    public boolean isFailure() {
        return getResultValue() != 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.result;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CommandResult other = (CommandResult) obj;
        if (result != other.result)
            return false;
        return true;
    }
}
