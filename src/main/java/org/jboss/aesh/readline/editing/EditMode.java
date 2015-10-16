/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.readline.editing;

import org.jboss.aesh.readline.Action;
import org.jboss.aesh.readline.KeyEvent;
import org.jboss.aesh.terminal.Key;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface EditMode {

    Action parse(Key event);

    enum Status {
        DELETE,
        MOVE,
        YANK,
        CHANGE,
        EDIT,
        COMMAND,
        SEARCH,
        IGNORE_EOF;
    }
}
