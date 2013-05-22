/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.complete;

/**
 * To enable auto completion, commands need to implement this interface.
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public interface Completion {

    /**
     * Populate the CompleteOperation object with possible
     * completions + offset if needed
     *
     * @param completeOperation operation
     */
    void complete(CompleteOperation completeOperation);
}
