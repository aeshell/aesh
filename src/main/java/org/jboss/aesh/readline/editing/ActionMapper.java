/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.readline.editing;

import org.jboss.aesh.readline.Action;
import org.jboss.aesh.readline.KeyEvent;
import org.jboss.aesh.readline.KeyMapper;
import org.jboss.aesh.readline.Keys;
import org.jboss.aesh.readline.actions.EndOfLine;
import org.jboss.aesh.readline.actions.Enter;
import org.jboss.aesh.readline.actions.NextHistory;
import org.jboss.aesh.readline.actions.PrevHistory;
import org.jboss.aesh.readline.actions.StartOfLine;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
class ActionMapper {

    private Map<KeyEvent,Action> mapping;

    public static ActionMapper getEmacs() {

        ActionMapper mapper = new ActionMapper();
        mapper.createEmacsMapping();
        return mapper;
    }

    private Map<KeyEvent, Action> createEmacsMapping() {
        mapping = new HashMap<>();
        KeyMapper mapper = new KeyMapper();

        mapping.put(Keys.CTRL_A, new StartOfLine());
        mapping.put(Keys.CTRL_E, new EndOfLine());
        mapping.put(Keys.CTRL_J, new Enter());
        mapping.put(mapper.getByName("up"), new PrevHistory());
        mapping.put(mapper.getByName("down"), new NextHistory());

        return mapping;
    }

    public Map<KeyEvent, Action> getMapping() {
        return mapping;
    }


}
