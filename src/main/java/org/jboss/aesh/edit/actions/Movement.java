/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit.actions;

/**
 * Describes possible movements
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public enum Movement {
    PREV,
    NEXT,
    BEGINNING,
    END,
    NEXT_WORD,
    PREV_WORD,
    NEXT_BIG_WORD,
    PREV_BIG_WORD,
    ALL;
}
