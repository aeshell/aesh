/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.edit.mapper;

import org.jboss.aesh.edit.actions.Operation;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class OperationMapper {

    public static Operation mapToFunction(String function) {

        if(function.equals("abort"))
            return Operation.ABORT;
        else if(function.equals("accept-line"))
            return Operation.NEW_LINE;
        else if(function.equals("backward-char"))
            return Operation.MOVE_PREV_CHAR;
        else if(function.equals("backward-delete-char"))
            return Operation.DELETE_PREV_CHAR;
        else if(function.equals("backward-kill-line"))
            return Operation.DELETE_BEGINNING;
        else if(function.equals("backward-kill-word"))
            return Operation.DELETE_PREV_WORD;
        else if(function.equals("backward-word"))
            return Operation.MOVE_PREV_WORD;
        else if(function.equals("beginning-of-history"))
            return Operation.HISTORY_NEXT; //TODO: need to add a proper Operation
        else if(function.equals("beginning-of-line"))
            return Operation.MOVE_BEGINNING;
        else if(function.equals("call-last-kbd-macro"))
            return Operation.NO_ACTION; //TODO: need to add a proper Operation
        else if(function.equals("capitalize-word"))
            return Operation.CASE; //TODO: need to add a proper Operation
        else if(function.equals("character-search"))
            return Operation.NO_ACTION; //TODO: need to add a proper Operation
        else if(function.equals("character-search-backward"))
            return Operation.NO_ACTION; //TODO: need to add a proper Operation
        else if(function.equals("clear-screen"))
            return Operation.CLEAR;
        else if(function.equals("complete"))
            return Operation.COMPLETE;
        else if(function.equals("copy-backward-word"))
            return Operation.YANK_PREV_WORD;
        else if(function.equals("copy-forward-word"))
            return Operation.YANK_NEXT_WORD;
        else if(function.equals("delete-char"))
            return Operation.DELETE_NEXT_CHAR;
        else if(function.equals("delete-char-or-list"))
            return Operation.NO_ACTION; //TODO: need to add a proper Operation
        else if(function.equals("delete-horizontal-space"))
            return Operation.NO_ACTION; //TODO: need to add a proper Operation
        else if(function.equals("digit-argument"))
            return Operation.NO_ACTION; //TODO: need to add a proper Operation
        else if(function.equals("do-uppercase-version"))
            return Operation.NO_ACTION; //TODO: need to add a proper Operation
        else if(function.equals("downcase-word"))
            return Operation.CASE; //TODO: need to add a proper Operation
        else if(function.equals("dump-functions"))
            return Operation.NO_ACTION; //TODO: need to add a proper Operation
        else if(function.equals("dump-macros"))
            return Operation.NO_ACTION; //TODO: need to add a proper Operation
        else if(function.equals("dump-variables"))
            return Operation.NO_ACTION; //TODO: need to add a proper Operation
        else if(function.equals("emacs-editing-mode"))
            return Operation.EMACS_EDIT_MODE;
        else if(function.equals("end-kbd-macro"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("end-of-history"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("end-of-line"))
            return Operation.MOVE_END;
        else if(function.equals("exchange-point-and-mark"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("forward-backward-delete-char"))
            return Operation.DELETE_NEXT_CHAR;
        else if(function.equals("forward-char"))
            return Operation.MOVE_NEXT_CHAR;
        else if(function.equals("forward-search-history"))
            return Operation.SEARCH_NEXT;
        else if(function.equals("forward-word"))
            return Operation.MOVE_NEXT_WORD;
        else if(function.equals("history-search-backward"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("history-search-forward"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("insert-comment"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("insert-comletions"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("kill-line"))
            return Operation.DELETE_END;
        else if(function.equals("kill-region"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("kill-whole-line"))
            return Operation.DELETE_ALL;
        else if(function.equals("kill-word"))
            return Operation.DELETE_NEXT_WORD;
        else if(function.equals("menu-complete"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("menu-complete-backward"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("next-history"))
            return Operation.HISTORY_NEXT;
        else if(function.equals("non-incremental-forward-search-history"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("non-incremental-reverse-search-history"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("overwrite-mode"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("possible-completions"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("prefix-meta"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("previous-history"))
            return Operation.HISTORY_PREV;
        else if(function.equals("quoted-insert"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("re-read-init-file"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("redraw-current-line"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("reverse-search-history"))
            return Operation.SEARCH_PREV;
        else if(function.equals("revert-line"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("self-insert"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("set-mark"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("skip-csi-sequence"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("start-kbd-macro"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("tab-insert"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("tilde-expand"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("tilde-expand"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("transpose-chars"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("transpose-words"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("undo"))
            return Operation.UNDO;
        else if(function.equals("universal-argument"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("unix-filename-rubout"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("unix-line-discard"))
            return Operation.DELETE_BEGINNING;
        else if(function.equals("unix-word-rubout"))
            return Operation.DELETE_PREV_BIG_WORD;
        else if(function.equals("upcase-word"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("vi-editing-mode"))
            return Operation.VI_EDIT_MODE;
        else if(function.equals("yank"))
            return Operation.PASTE_AFTER;
        else if(function.equals("yank-last-arg"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("yank-nth-arg"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation
        else if(function.equals("yank-pop"))
            return Operation.NO_ACTION; // TODO: need to add a proper Operation

        return Operation.NO_ACTION;
    }
}
