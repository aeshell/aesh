/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.readline.editing;

import org.jboss.aesh.readline.Action;
import org.jboss.aesh.readline.KeyEvent;
import org.jboss.aesh.readline.actions.BackwardChar;
import org.jboss.aesh.readline.actions.Complete;
import org.jboss.aesh.readline.actions.DeleteChar;
import org.jboss.aesh.readline.actions.DeletePrevChar;
import org.jboss.aesh.readline.actions.EndOfLine;
import org.jboss.aesh.readline.actions.Enter;
import org.jboss.aesh.readline.actions.ForwardChar;
import org.jboss.aesh.readline.actions.Interrupt;
import org.jboss.aesh.readline.actions.NextHistory;
import org.jboss.aesh.readline.actions.PrevHistory;
import org.jboss.aesh.readline.actions.StartOfLine;
import org.jboss.aesh.terminal.Key;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class EditModeMapper {

    private Map<KeyEvent,Action> mapping;

    public static EditModeMapper getEmacs() {
        EditModeMapper mapper = new EditModeMapper();
        mapper.createEmacsMapping();
        return mapper;
    }

    private Map<KeyEvent, Action> createEmacsMapping() {
        mapping = new HashMap<>();

        mapping.put(Key.CTRL_A, new StartOfLine());
        mapping.put(Key.CTRL_E, new EndOfLine());
        mapping.put(Key.CTRL_J, new Enter());
        mapping.put(Key.ENTER, new Enter());
        mapping.put(Key.UP, new PrevHistory());
        mapping.put(Key.UP_2, new PrevHistory());
        mapping.put(Key.DOWN, new NextHistory());
        mapping.put(Key.DOWN_2, new NextHistory());
        mapping.put(Key.LEFT, new BackwardChar());
        mapping.put(Key.LEFT_2, new BackwardChar());
        mapping.put(Key.RIGHT, new ForwardChar());
        mapping.put(Key.RIGHT_2, new ForwardChar());
        mapping.put(Key.BACKSPACE, new DeletePrevChar());
        mapping.put(Key.DELETE, new DeleteChar());
        mapping.put(Key.CTRL_I, new Complete());
        mapping.put(Key.CTRL_C, new Interrupt());

        return mapping;
    }

    public Map<KeyEvent, Action> getMapping() {
        return mapping;
    }

    @Override
    public String toString() {
        return "EditModeMapper{" +
                "mapping=" + mapping +
                '}';
    }
}
