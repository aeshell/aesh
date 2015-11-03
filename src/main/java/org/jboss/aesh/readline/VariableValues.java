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

package org.jboss.aesh.readline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public enum VariableValues {

    BELL_STYLE(Variable.BELL_STYLE, Arrays.asList("none", "visible", "audible")),
    BIND_TTY_SPECIAL_CHARS(Variable.BIND_TTY_SPECIAL_CHARS, Arrays.asList("on","off")),
    COMMENT_BEGIN(Variable.COMMENT_BEGIN, new ArrayList<String>()),
    COMPLETION_DISPLAY_WIDTH(Variable.COMPLETION_DISPLAY_WIDTH, new ArrayList<String>()),
    COMPLETION_IGNORE_CASE(Variable.COMPLETION_IGNORE_CASE, Arrays.asList("on","off")),
    COMPLETION_MAP_CASE(Variable.COMPLETION_MAP_CASE, Arrays.asList("on","off")),
    COMPLETION_PREFIX_DISPLAY_LENGTH(Variable.COMPLETION_PREFIX_DISPLAY_LENGTH, new ArrayList<String>()),
    COMPLETION_QUERY_ITEMS(Variable.COMPLETION_QUERY_ITEMS, new ArrayList<String>()),
    CONVERT_META(Variable.CONVERT_META, Arrays.asList("on","off")),
    DISABLE_COMPLETION(Variable.DISABLE_COMPLETION, Arrays.asList("on","off")),
    EDITING_MODE(Variable.EDITING_MODE, Arrays.asList("vi","emacs")),
    ECHO_CONTROL_CHARACTERS(Variable.ECHO_CONTROL_CHARACTERS, Arrays.asList("on","off")),
    ENABLE_KEYPAD(Variable.ENABLE_KEYPAD, Arrays.asList("on","off")),
    EXPAND_TILDE(Variable.EXPAND_TILDE, Arrays.asList("on","off")),
    HISTORY_PRESERVE_POINT(Variable.HISTORY_PRESERVE_POINT, Arrays.asList("on","off")),
    HISTORY_SIZE(Variable.HISTORY_SIZE, new ArrayList<String>()),
    HISTORY_SCROLL_MODE(Variable.HISTORY_SCROLL_MODE, Arrays.asList("on","off")),
    INPUT_META(Variable.INPUT_META, Arrays.asList("on","off")),
    ISEARCH_TERMINATORS(Variable.ISEARCH_TERMINATORS, new ArrayList<String>()),
    KEYMAP(Variable.KEYMAP, Arrays.asList("emacs","vi","emacs-standard","emacs-meta","emacs-ctlx","vi-move","vi-command","vi-insert")),
    MARK_DIRECTORIES(Variable.MARK_DIRECTORIES, Arrays.asList("on","off")),
    MARK_MODIFIED_LINES(Variable.MARK_MODIFIED_LINES, Arrays.asList("on","off")),
    MARK_SYMLINKED_DIRECTORIES(Variable.MARK_SYMLINKED_DIRECTORIES, Arrays.asList("on","off")),
    MATCH_HIDDEN_FILES(Variable.MATCH_HIDDEN_FILES, Arrays.asList("on","off")),
    MENU_COMPLETE_DISPLAY_PREFIX(Variable.MENU_COMPLETE_DISPLAY_PREFIX, Arrays.asList("on","off")),
    OUTPUT_META(Variable.OUTPUT_META, Arrays.asList("on","off")),
    PAGE_COMPLETIONS(Variable.PAGE_COMPLETIONS, Arrays.asList("on","off")),
    PRINT_COMPLETIONS_HORIZONTALLY(Variable.PRINT_COMPLETIONS_HORIZONTALLY, Arrays.asList("on","off")),
    REVERT_ALL_AT_NEWLINE(Variable.REVERT_ALL_AT_NEWLINE, Arrays.asList("on","off")),
    SHOW_ALL_IF_AMBIGUOUS(Variable.SHOW_ALL_IF_AMBIGUOUS, Arrays.asList("on","off")),
    SHOW_ALL_IF_UNMODIFIED(Variable.SHOW_ALL_IF_UNMODIFIED, Arrays.asList("on","off")),
    SKIP_COMPLETED_TEXT(Variable.SKIP_COMPLETED_TEXT, Arrays.asList("on","off")),
    VISIBLE_STATS(Variable.VISIBLE_STATS, Arrays.asList("on", "off"));

    private Variable variable;
    private List<String> values;

    VariableValues(Variable variable, List<String> values) {
        this.variable = variable;
        this.values = values;
    }

    public static List<String> getValuesByVariable(Variable variable) {
        for(VariableValues value : values()) {
            if(value.variable == variable)
                return value.values;
        }
        return new ArrayList<>();
    }

    public boolean hasValue() {
        return values.size() > 0;
    }
}
