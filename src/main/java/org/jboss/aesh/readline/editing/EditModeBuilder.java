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

import org.jboss.aesh.readline.Variable;
import org.jboss.aesh.readline.actions.BackwardChar;
import org.jboss.aesh.readline.actions.BeginningOfLine;
import org.jboss.aesh.readline.actions.CopyLine;
import org.jboss.aesh.readline.actions.DeleteBackwardBigWord;
import org.jboss.aesh.readline.actions.DeleteBackwardWord;
import org.jboss.aesh.readline.actions.DeleteChar;
import org.jboss.aesh.readline.actions.DeleteForwardBigWord;
import org.jboss.aesh.readline.actions.DeleteForwardWord;
import org.jboss.aesh.readline.actions.DeletePrevChar;
import org.jboss.aesh.readline.actions.DeleteStartOfLine;
import org.jboss.aesh.readline.actions.EndOfLine;
import org.jboss.aesh.readline.actions.ForwardChar;
import org.jboss.aesh.readline.actions.Interrupt;
import org.jboss.aesh.readline.actions.MoveBackwardBigWord;
import org.jboss.aesh.readline.actions.MoveBackwardWord;
import org.jboss.aesh.readline.actions.MoveForwardBigWord;
import org.jboss.aesh.readline.actions.MoveForwardWord;
import org.jboss.aesh.readline.actions.NextHistory;
import org.jboss.aesh.readline.actions.NoAction;
import org.jboss.aesh.readline.actions.PrevHistory;
import org.jboss.aesh.readline.actions.Undo;
import org.jboss.aesh.terminal.Key;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class EditModeBuilder {

    private Map<int[],String> actions;

    private Map<Variable,String> variables;

    public EditModeBuilder(EditMode.Mode mode) {
        this();
        if(mode == EditMode.Mode.EMACS)
            variables.put(Variable.EDITING_MODE, "emacs");
        else
            variables.put(Variable.EDITING_MODE, "vi");
    }

    public EditModeBuilder() {
        actions = new HashMap<>();
        variables = new EnumMap<>(Variable.class);
    }

    public EditModeBuilder addAction(int[] input, String action) {
        actions.put(input, action);
        return this;
    }

    public EditModeBuilder addVariable(Variable variable, String value) {
        variables.put(variable, value);
        return this;
    }

    public EditModeBuilder parseInputrc(InputStream inputStream) {
        InputrcParser.parseInputrc(inputStream, this);
        return this;
    }

    public String getVariableValue(Variable variable) {
        return variables.get(variable);
    }

    public EditMode create() {
        String mode = variables.getOrDefault(Variable.EDITING_MODE, "emacs");
        if(mode.equals("vi")) {
            EditMode editMode = createDefaultViMode();
            actions.forEach(editMode::addAction);
            variables.forEach(editMode::addVariable);
            return editMode;
        }
        else {
            EditMode editMode = createDefaultEmacsMode();
            actions.forEach(editMode::addAction);
            variables.forEach(editMode::addVariable);
            return editMode;
        }
    }

    private EditMode createDefaultEmacsMode() {
        Emacs emacs = new Emacs();

        emacs.addAction(Key.CTRL_A, "beginning-of-line");
        emacs.addAction(Key.CTRL_B, "backward-char");
        emacs.addAction(Key.CTRL_D, "delete-char");
        emacs.addAction(Key.CTRL_E, "end-of-line");
        emacs.addAction(Key.CTRL_F, "forward-char");
        emacs.addAction(Key.CTRL_H, "backward-delete-char");
        emacs.addAction(Key.CTRL_K, "kill-line");
        emacs.addAction(Key.CTRL_U, "unix-line-discard");
        emacs.addAction(Key.CTRL_J, "accept-line");
        emacs.addAction(Key.CTRL_M, "accept-line");
        emacs.addAction(Key.ENTER, "accept-line");
        emacs.addAction(Key.UP, "previous-history");
        emacs.addAction(Key.UP_2, "previous-history");
        emacs.addAction(Key.CTRL_P, "previous-history");
        emacs.addAction(Key.DOWN, "next-history");
        emacs.addAction(Key.DOWN_2, "next-history");
        emacs.addAction(Key.CTRL_N, "next-history");
        emacs.addAction(Key.LEFT, "backward-char");
        emacs.addAction(Key.LEFT_2, "backward-char");
        emacs.addAction(Key.RIGHT, "forward-char");
        emacs.addAction(Key.RIGHT_2, "forward-char");
        emacs.addAction(Key.BACKSPACE, "backward-delete-char");
        emacs.addAction(Key.DELETE, "delete-char");
        emacs.addAction(Key.CTRL_I, "complete");
        emacs.addAction(Key.CTRL_C, new Interrupt());
        emacs.addAction(Key.META_b, "backward-word");
        emacs.addAction(Key.META_c, "capitalize-word");
        emacs.addAction(Key.META_f, "forward-word");
        emacs.addAction(Key.META_d, "kill-word");
        emacs.addAction(Key.META_l, "downcase-word");
        emacs.addAction(Key.META_u, "upcase-word");
        emacs.addAction(Key.META_BACKSPACE, "backward-kill-word");
        emacs.addAction(Key.CTRL_W, "unix-word-rubout");
        emacs.addAction(Key.CTRL_U, "unix-line-discard");
        emacs.addAction(Key.CTRL_X_CTRL_U, "undo");
        emacs.addAction(Key.UNIT_SEPARATOR, "undo");
        emacs.addAction(Key.CTRL_R, "reverse-search-history");
        emacs.addAction(Key.CTRL_S, "forward-search-history");

        return emacs;
    }

    private EditMode createDefaultViMode() {
        Vi vi = new Vi();

        if(Key.ENTER.equals(Key.ENTER_2))
            vi.addAction(Key.ENTER, "accept-line");
        else {
            vi.addAction(Key.ENTER, "accept-line")
                    .addAction(Key.ENTER_2, "accept-line");
        }

        vi.addAction(Key.CTRL_C, new Interrupt());
        //TODO: EOF
        //vi.addAction(Key.CTRL_D, EOF);
        //TODO: change to emacs mode:
        //vi.addAction/Key.CTRL_E, emacs_edit_mode
        vi.addAction(Key.CTRL_I, "complete"); //tab
        vi.addAction(Key.CTRL_L, "clear-screen"); //ctrl-l
        vi.addAction(Key.CTRL_Z, new NoAction());

        //search
        vi.addAction(Key.CTRL_R, "reverse-search-history");
        vi.addAction(Key.CTRL_S, "forward-search-history");

        //edit
        vi.addActionGroup(Key.ESC, new Vi.ActionStatusGroup(new Vi.ActionStatus[]{
                new Vi.ActionStatus(new NoAction(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new NoAction(), EditMode.Status.CHANGE, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new NoAction(), EditMode.Status.DELETE, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new NoAction(), EditMode.Status.REPLACE, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new NoAction(), EditMode.Status.YANK, EditMode.Status.COMMAND),
        }));
        vi.addActionGroup(Key.d, new Vi.ActionStatusGroup(new Vi.ActionStatus[]{
                new Vi.ActionStatus(new NoAction(), EditMode.Status.COMMAND, EditMode.Status.DELETE),
                new Vi.ActionStatus("kill-whole-line", EditMode.Status.DELETE, EditMode.Status.COMMAND),
        }));
        vi.addActionGroup(Key.c, new Vi.ActionStatusGroup(new Vi.ActionStatus[]{
                new Vi.ActionStatus(new NoAction(), EditMode.Status.COMMAND, EditMode.Status.CHANGE),
                new Vi.ActionStatus("kill-whole-line", EditMode.Status.CHANGE, EditMode.Status.EDIT),
        }));
        vi.addActionGroup(Key.y, new Vi.ActionStatusGroup(new Vi.ActionStatus[]{
                new Vi.ActionStatus(new NoAction(), EditMode.Status.COMMAND, EditMode.Status.YANK),
                new Vi.ActionStatus(new CopyLine(), EditMode.Status.YANK, EditMode.Status.COMMAND),
        }));

        vi.addAction(Key.s, "delete-char", EditMode.Status.COMMAND, EditMode.Status.EDIT);
        vi.addAction(Key.S, "kill-whole-line", EditMode.Status.COMMAND, EditMode.Status.EDIT); //S
        vi.addAction(Key.D, "kill-line", EditMode.Status.COMMAND, EditMode.Status.COMMAND); //D
        vi.addAction(Key.C, "kill-line", EditMode.Status.COMMAND, EditMode.Status.EDIT ); //C
        vi.addAction(Key.a, "forward-char", EditMode.Status.COMMAND, EditMode.Status.EDIT);
        vi.addAction(Key.A, "end-of-line", EditMode.Status.COMMAND, EditMode.Status.EDIT); //A
        vi.addAction(Key.x, "delete-char", EditMode.Status.COMMAND, EditMode.Status.COMMAND); //x
        vi.addAction(Key.X, "backward-delete-char", EditMode.Status.COMMAND, EditMode.Status.COMMAND); //X
        vi.addAction(Key.p, "yank", EditMode.Status.COMMAND, EditMode.Status.COMMAND); //p
        vi.addAction(Key.P, "yank-after", EditMode.Status.COMMAND, EditMode.Status.COMMAND); //p
        vi.addAction(Key.i, new NoAction(), EditMode.Status.COMMAND, EditMode.Status.EDIT); //i
        vi.addAction(Key.I, "beginning-of-line", EditMode.Status.COMMAND, EditMode.Status.EDIT); //I
        vi.addAction(Key.TILDE, "upcase-char", EditMode.Status.COMMAND, EditMode.Status.COMMAND); //~

        //replace
        vi.addAction(Key.r, new NoAction(), EditMode.Status.COMMAND, EditMode.Status.REPLACE); //r

        //movement
        vi.addActionGroup(Key.h, new Vi.ActionStatusGroup( new Vi.ActionStatus[] {
                new Vi.ActionStatus(new BackwardChar(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new DeletePrevChar(), EditMode.Status.DELETE, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new DeletePrevChar(), EditMode.Status.CHANGE, EditMode.Status.EDIT),
        }));
        vi.addActionGroup(Key.l, new Vi.ActionStatusGroup(new Vi.ActionStatus[]{
                new Vi.ActionStatus(new ForwardChar(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new DeleteChar(), EditMode.Status.DELETE, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new DeleteChar(), EditMode.Status.CHANGE, EditMode.Status.EDIT),
        }));
        vi.addActionGroup(Key.b, new Vi.ActionStatusGroup(new Vi.ActionStatus[]{
                new Vi.ActionStatus(new MoveBackwardWord(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new DeleteBackwardWord(), EditMode.Status.DELETE, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new DeleteBackwardWord(), EditMode.Status.CHANGE, EditMode.Status.EDIT),
        }));
        vi.addActionGroup(Key.w, new Vi.ActionStatusGroup(new Vi.ActionStatus[]{
                new Vi.ActionStatus(new MoveForwardWord(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new DeleteForwardWord(), EditMode.Status.DELETE, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new DeleteForwardWord(), EditMode.Status.CHANGE, EditMode.Status.EDIT),
        }));
        vi.addActionGroup(Key.B, new Vi.ActionStatusGroup(new Vi.ActionStatus[]{
                new Vi.ActionStatus(new MoveBackwardBigWord(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new DeleteBackwardBigWord(), EditMode.Status.DELETE, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new DeleteBackwardBigWord(), EditMode.Status.CHANGE, EditMode.Status.EDIT),
        }));
        vi.addActionGroup(Key.W, new Vi.ActionStatusGroup(new Vi.ActionStatus[]{
                new Vi.ActionStatus(new MoveForwardBigWord(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new DeleteForwardBigWord(), EditMode.Status.DELETE, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new DeleteForwardBigWord(), EditMode.Status.CHANGE, EditMode.Status.EDIT),
        }));
        vi.addActionGroup(Key.ZERO, new Vi.ActionStatusGroup(new Vi.ActionStatus[]{
                new Vi.ActionStatus(new BeginningOfLine(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new DeleteStartOfLine(), EditMode.Status.DELETE, EditMode.Status.COMMAND),
                new Vi.ActionStatus(new DeleteStartOfLine(), EditMode.Status.CHANGE, EditMode.Status.EDIT),
        }));

        vi.addActionGroup(Key.DOLLAR, new Vi.ActionStatusGroup(new Vi.ActionStatus[]{
                new Vi.ActionStatus(new EndOfLine(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                new Vi.ActionStatus("kill-line", EditMode.Status.DELETE, EditMode.Status.COMMAND),
                new Vi.ActionStatus("kill-line", EditMode.Status.CHANGE, EditMode.Status.EDIT),
        }));

        vi.addAction(Key.k, "previous-history", EditMode.Status.COMMAND, EditMode.Status.COMMAND); //h
        vi.addAction(Key.j, "next-history", EditMode.Status.COMMAND, EditMode.Status.COMMAND); //l
        vi.addAction(Key.SPACE, "forward-char", EditMode.Status.COMMAND, EditMode.Status.COMMAND); //space

        //repeat
        vi.addAction(Key.PERIOD, new NoAction(), EditMode.Status.COMMAND, EditMode.Status.REPEAT); //.
        //undo
        vi.addAction(Key.u, new Undo(), EditMode.Status.COMMAND, EditMode.Status.COMMAND); //u
        //backspace
        vi.addAction(Key.BACKSPACE, "backward-delete-char", EditMode.Status.EDIT, EditMode.Status.EDIT);
        //movement
        if(Key.RIGHT.equalTo(Key.RIGHT_2.getKeyValues())) {
            vi.addActionGroup(Key.RIGHT, new Vi.ActionStatusGroup( new Vi.ActionStatus[] {
                    new Vi.ActionStatus(new ForwardChar(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                    new Vi.ActionStatus(new ForwardChar(), EditMode.Status.EDIT, EditMode.Status.EDIT) }));
        }
        else {
            vi.addActionGroup(Key.RIGHT, new Vi.ActionStatusGroup( new Vi.ActionStatus[] {
                    new Vi.ActionStatus(new ForwardChar(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                    new Vi.ActionStatus(new ForwardChar(), EditMode.Status.EDIT, EditMode.Status.EDIT) }));
            vi.addActionGroup(Key.RIGHT_2, new Vi.ActionStatusGroup( new Vi.ActionStatus[] {
                    new Vi.ActionStatus(new ForwardChar(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                    new Vi.ActionStatus(new ForwardChar(), EditMode.Status.EDIT, EditMode.Status.EDIT) }));
        }
        if(Key.LEFT.equalTo(Key.LEFT_2.getKeyValues())) {
            vi.addActionGroup(Key.LEFT, new Vi.ActionStatusGroup( new Vi.ActionStatus[] {
                    new Vi.ActionStatus(new BackwardChar(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                    new Vi.ActionStatus(new BackwardChar(), EditMode.Status.EDIT, EditMode.Status.EDIT) }));
        }
        else {
            vi.addActionGroup(Key.LEFT, new Vi.ActionStatusGroup( new Vi.ActionStatus[] {
                    new Vi.ActionStatus(new BackwardChar(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                    new Vi.ActionStatus(new BackwardChar(), EditMode.Status.EDIT, EditMode.Status.EDIT) }));
            vi.addActionGroup(Key.LEFT_2, new Vi.ActionStatusGroup( new Vi.ActionStatus[] {
                    new Vi.ActionStatus(new BackwardChar(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                    new Vi.ActionStatus(new BackwardChar(), EditMode.Status.EDIT, EditMode.Status.EDIT) }));
        }
        if(Key.UP.equalTo(Key.UP_2.getKeyValues())) {
            vi.addActionGroup(Key.UP, new Vi.ActionStatusGroup( new Vi.ActionStatus[] {
                    new Vi.ActionStatus(new PrevHistory(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                    new Vi.ActionStatus(new PrevHistory(), EditMode.Status.EDIT, EditMode.Status.EDIT) }));
        }
        else {
            vi.addActionGroup(Key.UP, new Vi.ActionStatusGroup( new Vi.ActionStatus[] {
                    new Vi.ActionStatus(new PrevHistory(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                    new Vi.ActionStatus(new PrevHistory(), EditMode.Status.EDIT, EditMode.Status.EDIT) }));
            vi.addActionGroup(Key.UP_2, new Vi.ActionStatusGroup( new Vi.ActionStatus[] {
                    new Vi.ActionStatus(new PrevHistory(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                    new Vi.ActionStatus(new PrevHistory(), EditMode.Status.EDIT, EditMode.Status.EDIT)
            }));

        }
        if(Key.DOWN.equalTo(Key.DOWN_2.getKeyValues())) {
            vi.addActionGroup(Key.DOWN, new Vi.ActionStatusGroup( new Vi.ActionStatus[] {
                    new Vi.ActionStatus(new NextHistory(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                    new Vi.ActionStatus(new NextHistory(), EditMode.Status.EDIT, EditMode.Status.EDIT) }));
        }
        else {
            vi.addActionGroup(Key.DOWN, new Vi.ActionStatusGroup( new Vi.ActionStatus[] {
                    new Vi.ActionStatus(new NextHistory(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                    new Vi.ActionStatus(new NextHistory(), EditMode.Status.EDIT, EditMode.Status.EDIT) }));
            vi.addActionGroup(Key.DOWN_2, new Vi.ActionStatusGroup( new Vi.ActionStatus[] {
                    new Vi.ActionStatus(new NextHistory(), EditMode.Status.COMMAND, EditMode.Status.COMMAND),
                    new Vi.ActionStatus(new NextHistory(), EditMode.Status.EDIT, EditMode.Status.EDIT) }));
        }
        if(Key.HOME.equalTo(Key.HOME_2.getKeyValues())) {
            vi.addAction(Key.HOME, new BeginningOfLine(), EditMode.Status.EDIT, EditMode.Status.EDIT);
        }
        else {
            vi.addAction(Key.HOME, new BeginningOfLine(), EditMode.Status.EDIT, EditMode.Status.EDIT);
            vi.addAction(Key.HOME_2, new BeginningOfLine(), EditMode.Status.EDIT, EditMode.Status.EDIT);
        }
        if(Key.END.equalTo(Key.END_2.getKeyValues())) {
            vi.addAction(Key.END, new EndOfLine(), EditMode.Status.EDIT, EditMode.Status.EDIT);
        }
        else {
            vi.addAction(Key.END, new EndOfLine(), EditMode.Status.EDIT, EditMode.Status.EDIT);
            vi.addAction(Key.END_2, new EndOfLine(), EditMode.Status.EDIT, EditMode.Status.EDIT);
        }

        vi.addAction(Key.CTRL_UP, new EndOfLine(), EditMode.Status.EDIT, EditMode.Status.EDIT);
        vi.addAction(Key.CTRL_DOWN, new BeginningOfLine(), EditMode.Status.EDIT, EditMode.Status.EDIT);
        vi.addAction(Key.CTRL_LEFT, new MoveBackwardBigWord(), EditMode.Status.EDIT, EditMode.Status.EDIT);
        vi.addAction(Key.CTRL_RIGHT, new MoveForwardBigWord(), EditMode.Status.EDIT, EditMode.Status.EDIT);

        vi.addAction(Key.DELETE, "delete-char", EditMode.Status.EDIT, EditMode.Status.EDIT);

        return vi;
    }

}
