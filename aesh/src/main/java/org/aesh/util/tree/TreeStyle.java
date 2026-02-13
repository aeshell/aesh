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
package org.aesh.util.tree;

/**
 * Predefined visual styles for tree rendering.
 *
 * <p>
 * Each style defines four connector strings used to draw the tree structure:
 * </p>
 * <ul>
 * <li>{@code branch} — prefix for non-last children</li>
 * <li>{@code last} — prefix for the last child</li>
 * <li>{@code vertical} — continuation line for non-last children</li>
 * <li>{@code space} — continuation line for last children</li>
 * </ul>
 *
 * <table>
 * <tr>
 * <th>Style</th>
 * <th>Example</th>
 * </tr>
 * <tr>
 * <td>ASCII</td>
 * <td>{@code +-- child}</td>
 * </tr>
 * <tr>
 * <td>UNICODE</td>
 * <td>{@code ├── child}</td>
 * </tr>
 * <tr>
 * <td>COMPACT</td>
 * <td>{@code ├─ child}</td>
 * </tr>
 * <tr>
 * <td>ROUNDED</td>
 * <td>{@code ╰── last-child}</td>
 * </tr>
 * </table>
 */
public enum TreeStyle {

    ASCII("+-- ", "\\-- ", "|   ", "    "),
    UNICODE("\u251C\u2500\u2500 ", "\u2514\u2500\u2500 ", "\u2502   ", "    "),
    COMPACT("\u251C\u2500 ", "\u2514\u2500 ", "\u2502  ", "   "),
    ROUNDED("\u251C\u2500\u2500 ", "\u2570\u2500\u2500 ", "\u2502   ", "    ");

    private final String branch;
    private final String last;
    private final String vertical;
    private final String space;

    TreeStyle(String branch, String last, String vertical, String space) {
        this.branch = branch;
        this.last = last;
        this.vertical = vertical;
        this.space = space;
    }

    public String branch() {
        return branch;
    }

    public String last() {
        return last;
    }

    public String vertical() {
        return vertical;
    }

    public String space() {
        return space;
    }
}
