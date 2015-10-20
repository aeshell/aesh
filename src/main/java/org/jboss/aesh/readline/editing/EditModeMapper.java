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
package org.jboss.aesh.readline.editing;

import org.jboss.aesh.readline.Action;
import org.jboss.aesh.readline.KeyEvent;
import org.jboss.aesh.readline.actions.BackwardChar;
import org.jboss.aesh.readline.actions.Complete;
import org.jboss.aesh.readline.actions.DeleteBackwardBigWord;
import org.jboss.aesh.readline.actions.DeleteChar;
import org.jboss.aesh.readline.actions.DeleteForwardWord;
import org.jboss.aesh.readline.actions.DeletePrevChar;
import org.jboss.aesh.readline.actions.EndOfLine;
import org.jboss.aesh.readline.actions.Enter;
import org.jboss.aesh.readline.actions.ForwardChar;
import org.jboss.aesh.readline.actions.Interrupt;
import org.jboss.aesh.readline.actions.MoveBackwardWord;
import org.jboss.aesh.readline.actions.MoveForwardWord;
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
        mapping.put(Key.META_b, new MoveBackwardWord());
        mapping.put(Key.META_f, new MoveForwardWord());
        mapping.put(Key.META_d, new DeleteForwardWord());
        mapping.put(Key.CTRL_W, new DeleteBackwardBigWord());


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
