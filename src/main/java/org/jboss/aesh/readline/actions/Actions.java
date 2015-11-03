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

package org.jboss.aesh.readline.actions;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public enum Actions {
    ABORT("abort"),
    ACCEPT_LINE("accept-line"),
    BACKWARD_CHAR("backward-char"),
    BACKWARD_DELETE_CHAR("backward-delete-char"),
    BACKWARD_KILL_LINE("backward-kill-line"),
    BACKWARD_KILL_WORD("backward-kill-word"),
    BACKWARD_WORD("backward-word"),
    BEGINNING_OF_HISTORY("beginning-of-history"),
    BEGINNING_OF_LINE("beginning-of-line"),
    CALL_LAST_KBD_MACRO("call-last-kbd-macro"),
    CAPITALIZE_WORD("capitalize-word"),
    CHARACTER_SEARCH("character-search"),
    CHARACTER_SEARCH_BACKWARD("character-search-backward"),
    CLEAR_SCREEN("clear-screen"),
    COMPLETE("complete"),
    COPY_BACKWARD_WORD("copy-backward-word"),
    COPY_FORWARD_WORD("copy-forward-word"),
    COPY_LINE("copy-line"),
    DELETE_CHAR("delete-char"),
    DELETE_CHAR_OR_LIST("delete-char-or-list"),
    DELETE_HORIZONTAL_SPACE("delete-horizontal-space"),
    DIGIT_ARGUMENT("digit_argument"),
    DO_UPPERCASE_VERSION("do-uppercase-version"),
    DOWNCASE_WORD("downcase-word"),
    DUMP_FUNCTIONS("dump-functions"),
    DUMP_MACROS("dump-macros"),
    DUMP_VARIABLES("dump-variables"),
    EMACS_EDITING_MODE("emacs-editing-mode"),
    END_KBD_MACRO("end-kbd-macro"),
    END_OF_HISTORY("end-of-history"),
    END_OF_LINE("end-of-line"),
    EXCHANGE_POINT_AND_MARK("exchange-point-and-mark"),
    FORWARD_BACKWARD_DELETE_CHAR("forward-backward-delete-char"),
    FORWARD_CHAR("forward-char"),
    FORWARD_SEARCH_HISTORY("forward-search-history"),
    FORWARD_WORD("forward-word"),
    FORWARD_SEARCH_BACKWARD("forward-search-backward"),
    HISTORY_SEARCH_FORWARD("history-search-forward"),
    INSERT_COMMENT("insert-comment"),
    INSERT_COMPLETIONS("insert-completions"),
    KILL_LINE("kill-line"),
    KILL_REGION("kill-region"),
    KILL_WHOLE_LINE("kill-whole-line"),
    KILL_WORD("kill-word"),
    MENU_COMPLETE("menu-complete"),
    MENU_COMPLETE_BACKWARD("menu-complete-backward"),
    NEXT_HISTORY("next-history"),
    NON_INCREMENTAL_FORWARD_SEARCH_HISTORY("non-incremental-forward-search-history"),
    NON_INCREMENTAL_REVERSE_SEARCH_HISTORY("non-incremental-reverse-search-history"),
    OVERWRITE_MODE("overwrite-mode"),
    POSSIBLE_COMPLETIONS("possible-completions"),
    PREFIX_META("prefix-meta"),
    PREVIOUS_HISTORY("previous-history"),
    QUOTED_INSERT("quoted-insert"),
    RE_READ_INIT_FILE("re-read-init-file"),
    REDRAW_CURRENT_LINE("redraw-current-line"),
    REVERSE_SEARCH_HISTORY("reverse-search-history"),
    REVERT_LINE("revert-line"),
    SELF_INSERT("self-insert"),
    SET_MARK("set-mark"),
    SKIP_CSI_SEQUENCE("skip-csi-sequence"),
    START_KBD_MACRO("start-kbd-macro"),
    TAB_INSERT("tab-insert"),
    TILDE_EXPAND("tilde-expand"),
    TRANSPOSE_CHARS("transpose-chars"),
    TRANSPOSE_WORDS("transpose-words"),
    UNDO("undo"),
    UNIVERSAL_ARGUMENT("universal-argument"),
    UNIX_FILENAME_RUBOUT("unix-filename-rubout"),
    UPCASE_CHAR("upcase-char"),
    UPCASE_WORD("upcase-word"),
    VI_EDITING_MODE("vi-editing-mode"),
    YANK("yank"),
    YANK_LAST_ARG("yank-last-arg"),
    YANK_NTH_ARG("yank-nth-arg"),
    YANK_POP("yank-pop"),
    YANK_AFTER("yank-after");

    private final String name;

    Actions(String name) {
        this.name = name;
    }
}
