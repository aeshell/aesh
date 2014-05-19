/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Defines all the possible variable settings defined for GNU Readline,
 * ref: http://cnswww.cns.cwru.edu/php/chet/readline/readline.html#SEC10
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public enum VariableSettings {

    BELL_STYLE("bell-style", Arrays.asList("none", "visible", "audible")),
    BIND_TTY_SPECIAL_CHARS("bind-tty-special-chars", Arrays.asList("on","off")),
    COMMENT_BEGIN("comment-begin", new ArrayList<String>()),
    COMPLETION_DISPLAY_WIDTH("completion-display-width", new ArrayList<String>()),
    COMPLETION_IGNORE_CASE("completion-ignore-case", Arrays.asList("on","off")),
    COMPLETION_MAP_CASE("completion-map-case", Arrays.asList("on","off")),
    COMPLETION_PREFIX_DISPLAY_LENGTH("completion-prefix-display-length", new ArrayList<String>()),
    COMPLETION_QUERY_ITEMS("completion-query-items", new ArrayList<String>()),
    CONVERT_META("convert-meta", Arrays.asList("on","off")),
    DISABLE_COMPLETION("disable-completion", Arrays.asList("on","off")),
    EDITING_MODE("editing-mode", Arrays.asList("vi","emacs")),
    ECHO_CONTROL_CHARACTERS("echo-control-characters", Arrays.asList("on","off")),
    ENABLE_KEYPAD("enable-keypad", Arrays.asList("on","off")),
    EXPAND_TILDE("expand-tilde", Arrays.asList("on","off")),
    HISTORY_PRESERVE_POINT("history-preserve-point", Arrays.asList("on","off")),
    HISTORY_SIZE("history-size", new ArrayList<String>()),
    HISTORY_SCROLL_MODE("history-scroll-mode", Arrays.asList("on","off")),
    INPUT_META("input-meta", Arrays.asList("on","off")),
    ISEARCH_TERMINATORS("isearch-terminators", new ArrayList<String>()),
    KEYMAP("keymap", Arrays.asList("emacs","vi","emacs-standard","emacs-meta","emacs-ctlx","vi-move","vi-command","vi-insert")),
    MARK_DIRECTORIES("mark-directories", Arrays.asList("on","off")),
    MARK_MODIFIED_LINES("mark-modified-lines", Arrays.asList("on","off")),
    MARK_SYMLINKED_DIRECTORIES("mark-symlinked-directories", Arrays.asList("on","off")),
    MATCH_HIDDEN_FILES("match-hidden-files", Arrays.asList("on","off")),
    MENU_COMPLETE_DISPLAY_PREFIX("menu-complete-display-prefix", Arrays.asList("on","off")),
    OUTPUT_META("output-meta", Arrays.asList("on","off")),
    PAGE_COMPLETIONS("page-completions", Arrays.asList("on","off")),
    PRINT_COMPLETIONS_HORIZONTALLY("print-completions-horizontally", Arrays.asList("on","off")),
    REVERT_ALL_AT_NEWLINE("revert-all-at-newline", Arrays.asList("on","off")),
    SHOW_ALL_IF_AMBIGUOUS("show-all-if-ambiguous", Arrays.asList("on","off")),
    SHOW_ALL_IF_UNMODIFIED("show-all-if-unmodified", Arrays.asList("on","off")),
    SKIP_COMPLETED_TEXT("skip-completed-text", Arrays.asList("on","off")),
    VISIBLE_STATS("visible-stats", Arrays.asList("on","off"));

    private String variable;
    private List<String> values;

    private VariableSettings(String variable, List<String> values) {
        this.variable = variable;
        this.values = values;
    }

    public String getVariable() {
        return variable;
    }

    public List<String> getValues() {
        return values;
    }

}
