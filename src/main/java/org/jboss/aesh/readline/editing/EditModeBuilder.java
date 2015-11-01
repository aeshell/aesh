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
            EditMode editMode = new Emacs();
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

}
