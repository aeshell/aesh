/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019 Red Hat Inc. and/or its affiliates and other contributors
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
package org.aesh.selector;

public class SelectLine {

    private final int maxLength;
    private boolean selected = false;
    private boolean focus = false;
    private final String description;

    public SelectLine(String description, int maxLength) {
        this.description = description;
        this.maxLength = maxLength;
    }

    public SelectLine(String description, boolean selected, int maxLength) {
        this.description = description;
        this.maxLength = maxLength;
        this.selected = selected;
    }

    public void select() {
        selected = !selected;
    }

    public void focus() {
        focus = !focus;
    }

    private char getFocus() {
        return focus ? '>' : ' ';
    }

    public boolean isSelected() {
        return selected;
    }

    public String print() {
        String out = selected ? getFocus()+"[*] "+description : getFocus()+"[ ] "+description;
        if(out.length() > maxLength)
            return out.substring(0, maxLength-3)+"...";
        else
            return out;
    }

    public String value() {
        return description;
    }
}
