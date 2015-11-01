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

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public enum Variable {

    BELL_STYLE("bell-style"),
    BIND_TTY_SPECIAL_CHARS("bind-tty-special-chars"),
    COMMENT_BEGIN("comment-begin"),
    COMPLETION_DISPLAY_WIDTH("completion-display-width"),
    COMPLETION_IGNORE_CASE("completion-ignore-case"),
    COMPLETION_MAP_CASE("completion-map-case"),
    COMPLETION_PREFIX_DISPLAY_LENGTH("completion-prefix-display-length"),
    COMPLETION_QUERY_ITEMS("completion-query-items"),
    CONVERT_META("convert-meta"),
    DISABLE_COMPLETION("disable-completion"),
    EDITING_MODE("editing-mode"),
    ECHO_CONTROL_CHARACTERS("echo-control-characters"),
    ENABLE_KEYPAD("enable-keypad"),
    EXPAND_TILDE("expand-tilde"),
    HISTORY_PRESERVE_POINT("history-preserve-point"),
    HISTORY_SIZE("history-size"),
    HISTORY_SCROLL_MODE("history-scroll-mode"),
    INPUT_META("input-meta"),
    ISEARCH_TERMINATORS("isearch-terminators"),
    KEYMAP("keymap"),
    MARK_DIRECTORIES("mark-directories"),
    MARK_MODIFIED_LINES("mark-modified-lines"),
    MARK_SYMLINKED_DIRECTORIES("mark-symlinked-directories"),
    MATCH_HIDDEN_FILES("match-hidden-files"),
    MENU_COMPLETE_DISPLAY_PREFIX("menu-complete-display-prefix"),
    OUTPUT_META("output-meta"),
    PAGE_COMPLETIONS("page-completions"),
    PRINT_COMPLETIONS_HORIZONTALLY("print-completions-horizontally"),
    REVERT_ALL_AT_NEWLINE("revert-all-at-newline"),
    SHOW_ALL_IF_AMBIGUOUS("show-all-if-ambiguous"),
    SHOW_ALL_IF_UNMODIFIED("show-all-if-unmodified"),
    SKIP_COMPLETED_TEXT("skip-completed-text"),
    VISIBLE_STATS("visible-stats");

    private String value;

    Variable(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Variable findVariable(String value) {
        if(value == null)
            return null;
        for(Variable variable : values()) {
            if(variable.getValue().equals(value))
                return variable;
        }
        return null;
    }

}
