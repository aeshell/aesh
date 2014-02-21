/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit.actions;

/**
 * User actions
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public enum Action {
    DELETE,
    MOVE,
    YANK,
    CHANGE,
    EDIT,
    COMMAND,
    HISTORY,
    SEARCH,
    // MISC
    NEWLINE,
    PASTE,
    PASTE_FROM_CLIPBOARD,
    COMPLETE,
    UNDO,
    CASE,
    EXIT,
    CLEAR,
    ABORT,
    CHANGE_EDITMODE,
    NO_ACTION,
    REPLACE,
    INTERRUPT,
    IGNOREEOF,
    EOF
}
