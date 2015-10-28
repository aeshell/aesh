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
import org.jboss.aesh.readline.actions.ActionMapper;
import org.jboss.aesh.readline.actions.Interrupt;
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

        mapping.put(Key.CTRL_A, ActionMapper.mapToAction("beginning-of-line"));
        mapping.put(Key.CTRL_B, ActionMapper.mapToAction("backward-char"));
        mapping.put(Key.CTRL_D, ActionMapper.mapToAction("delete-char"));
        mapping.put(Key.CTRL_E, ActionMapper.mapToAction("end-of-line"));
        mapping.put(Key.CTRL_F, ActionMapper.mapToAction("forward-char"));
        mapping.put(Key.CTRL_H, ActionMapper.mapToAction("backward-delete-char"));
        mapping.put(Key.CTRL_K, ActionMapper.mapToAction("kill-line"));
        mapping.put(Key.CTRL_U, ActionMapper.mapToAction("unix-line-discard"));
        mapping.put(Key.CTRL_J, ActionMapper.mapToAction("accept-line"));
        mapping.put(Key.ENTER, ActionMapper.mapToAction("accept-line"));
        mapping.put(Key.UP, ActionMapper.mapToAction("previous-history"));
        mapping.put(Key.UP_2, ActionMapper.mapToAction("previous-history"));
        mapping.put(Key.CTRL_P, ActionMapper.mapToAction("previous-history"));
        mapping.put(Key.DOWN, ActionMapper.mapToAction("next-history"));
        mapping.put(Key.DOWN_2, ActionMapper.mapToAction("next-history"));
        mapping.put(Key.CTRL_N, ActionMapper.mapToAction("next-history"));
        mapping.put(Key.LEFT, ActionMapper.mapToAction("backward-char"));
        mapping.put(Key.LEFT_2, ActionMapper.mapToAction("backward-char"));
        mapping.put(Key.RIGHT, ActionMapper.mapToAction("forward-char"));
        mapping.put(Key.RIGHT_2, ActionMapper.mapToAction("forward-char"));
        mapping.put(Key.BACKSPACE, ActionMapper.mapToAction("backward-delete-char"));
        mapping.put(Key.DELETE, ActionMapper.mapToAction("delete-char"));
        mapping.put(Key.CTRL_I, ActionMapper.mapToAction("complete"));
        mapping.put(Key.CTRL_C, new Interrupt());
        mapping.put(Key.META_b, ActionMapper.mapToAction("backward-word"));
        mapping.put(Key.META_c, ActionMapper.mapToAction("capitalize-word"));
        mapping.put(Key.META_f, ActionMapper.mapToAction("forward-word"));
        mapping.put(Key.META_d, ActionMapper.mapToAction("kill-word"));
        mapping.put(Key.META_l, ActionMapper.mapToAction("downcase-word"));
        mapping.put(Key.META_u, ActionMapper.mapToAction("upcase-word"));
        mapping.put(Key.META_BACKSPACE, ActionMapper.mapToAction("backward-kill-word"));
        mapping.put(Key.CTRL_W, ActionMapper.mapToAction("unix-word-rubout"));
        mapping.put(Key.CTRL_U, ActionMapper.mapToAction("unix-line-discard"));
        mapping.put(Key.CTRL_X_CTRL_U, ActionMapper.mapToAction("undo"));
        mapping.put(Key.UNIT_SEPARATOR, ActionMapper.mapToAction("undo"));
        mapping.put(Key.CTRL_R, ActionMapper.mapToAction("reverse-search-history"));

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
