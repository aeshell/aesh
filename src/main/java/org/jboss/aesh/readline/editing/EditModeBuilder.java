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
import org.jboss.aesh.readline.actions.Interrupt;
import org.jboss.aesh.readline.actions.NoAction;
import org.jboss.aesh.terminal.Key;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class EditModeBuilder {

    private Map<int[],String> actions;

    private Map<Variable,String> variables;


    public EditModeBuilder() {
        actions = new HashMap<>();
        variables = new HashMap<>();
    }

    public EditModeBuilder addAction(int[] input, String action) {
        actions.replace(input, action);
        return this;
    }

    public EditModeBuilder addVariable(Variable variable, String value) {
        variables.replace(variable, value);
        return this;
    }

    public EditMode create() {
        String mode = variables.getOrDefault(Variable.EDITING_MODE, "emacs");
        if(mode.equals("vi")) {
            //TODO: create vi mode, for now, just return emacs
            EditMode editMode = createDefaultViMode();
            actions.forEach(editMode::addAction);
            variables.forEach(editMode::addVariable);
            return editMode;
        }
        else {
            EditMode editMode = new Emacs();
            actions.forEach(editMode::addAction);
            variables.forEach(editMode::addVariable);
            return editMode;
        }
    }

    public EditMode createDefaultViMode() {
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
        /*
        vi.addAction(Key.ESC, Operation.ESCAPE)); //escape

        vi.addAction(Key.s, Operation.CHANGE_NEXT_CHAR)); //s
        vi.addAction(Key.S, Operation.CHANGE_ALL)); //S
        vi.addAction(Key.d, Operation.DELETE_ALL)); //d
        vi.addAction(Key.D, Operation.DELETE_END)); //D
        vi.addAction(Key.c, Operation.CHANGE)); //c
        vi.addAction(Key.C, Operation.CHANGE_END)); //C
        vi.addAction(Key.a, Operation.MOVE_NEXT_CHAR)); //a
        vi.addAction(Key.A, Operation.MOVE_END)); //A
        vi.addAction(Key.ZERO, Operation.BEGINNING)); //0
        vi.addAction(Key.DOLLAR, Operation.END)); //$
        vi.addAction(Key.x, Operation.DELETE_NEXT_CHAR)); //x
        vi.addAction(Key.X, Operation.DELETE_PREV_CHAR, Action.COMMAND)); //X
        vi.addAction(Key.p, Operation.PASTE_AFTER)); //p
        vi.addAction(Key.P, Operation.PASTE_BEFORE)); //P
        vi.addAction(Key.i, Operation.INSERT)); //i
        vi.addAction(Key.I, Operation.INSERT_BEGINNING)); //I
        vi.addAction(Key.TILDE, Operation.CASE)); //~
        vi.addAction(Key.y, Operation.YANK_ALL)); //y

        //replace
        vi.addAction(Key.r, Operation.REPLACE)); //r

        //movement
        vi.addAction(Key.h, Operation.PREV_CHAR)); //h
        vi.addAction(Key.l, Operation.NEXT_CHAR)); //l
        vi.addAction(Key.j, Operation.HISTORY_NEXT)); //j
        vi.addAction(Key.k, Operation.HISTORY_PREV)); //k
        vi.addAction(Key.b, Operation.PREV_WORD)); //b
        vi.addAction(Key.B, Operation.PREV_BIG_WORD)); //B
        vi.addAction(Key.w, Operation.NEXT_WORD)); //w
        vi.addAction(Key.W, Operation.NEXT_BIG_WORD)); //W
        vi.addAction(Key.SPACE, Operation.NEXT_CHAR)); //space

        //repeat
        vi.addAction(Key.PERIOD, Operation.REPEAT)); //.
        //undo
        vi.addAction(Key.u, Operation.UNDO)); //u
        //backspace
        vi.addAction(Key.BACKSPACE, Operation.DELETE_PREV_CHAR));
        */

        return null;
    }

}
