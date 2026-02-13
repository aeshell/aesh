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
package org.aesh.util.progress;

/**
 * Predefined visual styles for the progress bar.
 *
 * <p>
 * Each style defines the characters used for the filled portion,
 * empty portion, brackets, and an optional tip character.
 * </p>
 *
 * <table>
 * <tr>
 * <th>Style</th>
 * <th>Example</th>
 * </tr>
 * <tr>
 * <td>ASCII</td>
 * <td>{@code [####------] 40%}</td>
 * </tr>
 * <tr>
 * <td>UNICODE</td>
 * <td>{@code │████░░░░░░│ 40%}</td>
 * </tr>
 * <tr>
 * <td>SIMPLE</td>
 * <td>{@code [====      ] 40%}</td>
 * </tr>
 * <tr>
 * <td>ARROW</td>
 * <td>{@code [===>      ] 40%}</td>
 * </tr>
 * </table>
 */
public enum ProgressBarStyle {

    ASCII('#', '-', '[', ']', '#'),
    UNICODE('\u2588', '\u2591', '\u2502', '\u2502', '\u2588'),
    SIMPLE('=', ' ', '[', ']', '='),
    ARROW('=', ' ', '[', ']', '>');

    private final char fillChar;
    private final char emptyChar;
    private final char leftBracket;
    private final char rightBracket;
    private final char tipChar;

    ProgressBarStyle(char fillChar, char emptyChar, char leftBracket, char rightBracket, char tipChar) {
        this.fillChar = fillChar;
        this.emptyChar = emptyChar;
        this.leftBracket = leftBracket;
        this.rightBracket = rightBracket;
        this.tipChar = tipChar;
    }

    public char fillChar() {
        return fillChar;
    }

    public char emptyChar() {
        return emptyChar;
    }

    public char leftBracket() {
        return leftBracket;
    }

    public char rightBracket() {
        return rightBracket;
    }

    public char tipChar() {
        return tipChar;
    }
}
