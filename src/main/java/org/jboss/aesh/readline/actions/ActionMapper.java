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

import org.jboss.aesh.console.InputProcessor;
import org.jboss.aesh.readline.Action;

/**
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class ActionMapper {

    public static Action mapToAction(String function) {

        if(function.equals("abort"))
            return new NullAction();
        else if(function.equals("accept-line"))
            return new Enter();
        else if(function.equals("backward-char"))
            return new BackwardChar();
        else if(function.equals("backward-delete-char"))
            return new DeletePrevChar();
        else if(function.equals("delete-backward-char"))
            return new DeletePrevChar();
        else if(function.equals("backward-kill-line"))
            return new DeleteStartOfLine();
        else if(function.equals("backward-kill-word"))
            return new DeleteBackwardWord();
        else if(function.equals("backward-word"))
            return new MoveBackwardWord();
        else if(function.equals("beginning-of-history"))
            return new NextHistory(); //TODO: need to add a proper Operation
        else if(function.equals("beginning-of-line"))
            return new BeginningOfLine();
        else if(function.equals("call-last-kbd-macro"))
            return new NullAction(); //TODO: need to add a proper Operation
        else if(function.equals("capitalize-word"))
            return new CapitalizeForwardWord();
        else if(function.equals("character-search"))
            return new NullAction(); //TODO: need to add a proper Operation
        else if(function.equals("character-search-backward"))
            return new NullAction(); //TODO: need to add a proper Operation
        else if(function.equals("clear-screen"))
            return new Clear();
        else if(function.equals("complete"))
            return new Complete();
        else if(function.equals("copy-backward-word"))
            return new YankBackwardWord();
        else if(function.equals("copy-forward-word"))
            return new YankForwardWord();
        else if(function.equals("delete-char"))
            return new DeleteChar();
        else if(function.equals("delete-char-or-list"))
            return new NullAction(); //TODO: need to add a proper Operation
        else if(function.equals("delete-horizontal-space"))
            return new NullAction(); //TODO: need to add a proper Operation
        else if(function.equals("digit-argument"))
            return new NullAction(); //TODO: need to add a proper Operation
        else if(function.equals("do-uppercase-version"))
            return new NullAction(); //TODO: need to add a proper Operation
        else if(function.equals("downcase-word"))
            return new DownCaseForwardWord();
        else if(function.equals("dump-functions"))
            return new NullAction(); //TODO: need to add a proper Operation
        else if(function.equals("dump-macros"))
            return new NullAction(); //TODO: need to add a proper Operation
        else if(function.equals("dump-variables"))
            return new NullAction(); //TODO: need to add a proper Operation
        else if(function.equals("emacs-editing-mode"))
            return new EmacsEditingMode();
        else if(function.equals("end-kbd-macro"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("end-of-history"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("end-of-line"))
            return new EndOfLine();
        else if(function.equals("exchange-point-and-mark"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("forward-backward-delete-char"))
            return new DeleteChar(); //TODO: need a proper impl
        else if(function.equals("forward-char"))
            return new ForwardChar();
        else if(function.equals("forward-search-history"))
            return new ForwardSearchHistory();
        else if(function.equals("forward-word"))
            return new MoveForwardWord();
        else if(function.equals("history-search-backward"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("history-search-forward"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("insert-comment"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("insert-comletions"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("kill-line"))
            return new DeleteEndOfLine();
        else if(function.equals("kill-region"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("kill-whole-line"))
            return new DeleteLine();
        else if(function.equals("kill-word"))
            return new DeleteForwardWord();
        else if(function.equals("menu-complete"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("menu-complete-backward"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("next-history"))
            return new NextHistory();
        else if(function.equals("non-incremental-forward-search-history"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("non-incremental-reverse-search-history"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("overwrite-mode"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("possible-completions"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("prefix-meta"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("previous-history"))
            return new PrevHistory();
        else if(function.equals("quoted-insert"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("re-read-init-file"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("redraw-current-line"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("reverse-search-history"))
            return new ReverseSearchHistory();
        else if(function.equals("revert-line"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("self-insert"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("set-mark"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("skip-csi-sequence"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("start-kbd-macro"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("tab-insert"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("tilde-expand"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("tilde-expand"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("transpose-chars"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("transpose-words"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("undo"))
            return new Undo();
        else if(function.equals("universal-argument"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("unix-filename-rubout"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("unix-line-discard"))
            return new DeleteStartOfLine();
        else if(function.equals("unix-word-rubout"))
            return new DeleteBackwardBigWord();
        else if(function.equals("upcase-word"))
            return new UpCaseForwardWord();
        else if(function.equals("vi-editing-mode"))
            return new ViEditingMode();
        else if(function.equals("yank"))
            return new Yank();
        else if(function.equals("yank-last-arg"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("yank-nth-arg"))
            return new NullAction(); // TODO: need to add a proper Operation
        else if(function.equals("yank-pop"))
            return new NullAction(); // TODO: need to add a proper Operation

        return new NullAction();
    }

    private static class NullAction implements Action {

        @Override
        public String name() {
            return "no-action";
        }

        @Override
        public void apply(InputProcessor inputProcessor) {
        }
    }
}
