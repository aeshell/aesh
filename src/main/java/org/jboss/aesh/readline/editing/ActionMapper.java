/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.readline.editing;

import org.jboss.aesh.readline.KeyEvent;
import org.jboss.aesh.readline.KeyMapper;
import org.jboss.aesh.readline.Keys;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
class ActionMapper {

    private Map<KeyEvent, ActionEvent> mapping;

    public static ActionMapper getEmacs() {

        return emacs;
    }

    private Map<KeyEvent, ActionEvent> createEmacsMapping() {
        mapping = new HashMap<>();

        mapping.put(Keys.CTRL_A,  )


    }


}
