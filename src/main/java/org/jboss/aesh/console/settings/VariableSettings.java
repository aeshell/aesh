/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines all the possible variable settings defined for GNU Readline,
 * ref: http://cnswww.cns.cwru.edu/php/chet/readline/readline.html#SEC10
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public enum VariableSettings {

    BELL_STYLE("bell-style", new ArrayList<String>() {{ add("none"); add("visible"); add("audible"); }}),
    BIND_TTY_SPECIAL_CHARS("bind-tty-special-chars", new ArrayList<String>() {{add("on"); add("off"); }}),
    COMMENT_BEGIN("comment-begin", new ArrayList<String>()),
    COMPLETION_DISPLAY_WIDTH("completion-display-width", new ArrayList<String>()),
    COMPLETION_IGNORE_CASE("completion-ignore-case", new ArrayList<String>() {{ add("on"); add("off");}}),
    COMPLETION_MAP_CASE("completion-map-case", new ArrayList<String>() {{ add("on"); add("off");}}),
    COMPLETION_PREFIX_DISPLAY_LENGTH("completion-prefix-display-length", new ArrayList<String>()),
    COMPLETION_QUERY_ITEMS("completion-query-items", new ArrayList<String>()),
    CONVERT_META("convert-meta", new ArrayList<String>() {{ add("on"); add("off");}}),
    DISABLE_COMPLETION("disable-completion", new ArrayList<String>() {{ add("on"); add("off");}}),
    EDITING_MODE("editing-mode", new ArrayList<String>() {{ add("vi"); add("emacs"); }}),
    ECHO_CONTROL_CHARACTERS("echo-control-characters", new ArrayList<String>() {{ add("on"); add("off"); }}),
    ENABLE_KEYPAD("enable-keypad", new ArrayList<String>() {{ add("on"); add("off"); }}),
    EXPAND_TILDE("expand-tilde", new ArrayList<String>() {{ add("on"); add("off"); }}),
    HISTORY_PRESERVE_POINT("history-preserve-point", new ArrayList<String>() {{ add("on"); add("off"); }}),
    HISTORY_SIZE("history-size", new ArrayList<String>()),
    HISTORY_SCROLL_MODE("history-scroll-mode", new ArrayList<String>() {{ add("on"); add("off");}}),
    INPUT_META("input-meta", new ArrayList<String>() {{ add("on"); add("off");}}),
    ISEARCH_TERMINATORS("isearch-terminators", new ArrayList<String>()),
    KEYMAP("keymap", new ArrayList<String>() {{ add("emacs"); add("vi"); add("emacs-standard");
        add("emacs-meta"); add("emacs-ctlx"); add("vi-move"); add("vi-command"); add("vi-insert");}}),
    MARK_DIRECTORIES("mark-directories", new ArrayList<String>() {{ add("on"); add("off");}}),
    MARK_MODIFIED_LINES("mark-modified-lines", new ArrayList<String>() {{ add("on"); add("off");}}),
    MARK_SYMLINKED_DIRECTORIES("mark-symlinked-directories", new ArrayList<String>() {{ add("on"); add("off");}}),
    MATCH_HIDDEN_FILES("match-hidden-files", new ArrayList<String>() {{ add("on"); add("off");}}),
    MENU_COMPLETE_DISPLAY_PREFIX("menu-complete-display-prefix", new ArrayList<String>() {{ add("on"); add("off");}}),
    OUTPUT_META("output-meta", new ArrayList<String>() {{ add("on"); add("off");}}),
    PAGE_COMPLETIONS("page-completions", new ArrayList<String>() {{ add("on"); add("off");}}),
    PRINT_COMPLETIONS_HORIZONTALLY("print-completions-horizontally", new ArrayList<String>() {{ add("on"); add("off");}}),
    REVERT_ALL_AT_NEWLINE("revert-all-at-newline", new ArrayList<String>() {{ add("on"); add("off");}}),
    SHOW_ALL_IF_AMBIGUOUS("show-all-if-ambiguous", new ArrayList<String>() {{ add("on"); add("off");}}),
    SHOW_ALL_IF_UNMODIFIED("show-all-if-unmodified", new ArrayList<String>() {{ add("on"); add("off");}}),
    SKIP_COMPLETED_TEXT("skip-completed-text", new ArrayList<String>() {{ add("on"); add("off");}}),
    VISIBLE_STATS("visible-stats", new ArrayList<String>() {{ add("on"); add("off");}});

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
