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
package org.aesh.util.graph;

/**
 * Predefined visual styles for DAG graph rendering.
 *
 * <p>
 * Each style defines eleven box-drawing characters used to render edges
 * on a 2D character grid:
 * </p>
 * <ul>
 * <li>{@code horizontal} — horizontal edge ({@code ─})</li>
 * <li>{@code vertical} — vertical edge ({@code │})</li>
 * <li>{@code downTee} — parent splits down+left+right ({@code ┬})</li>
 * <li>{@code upTee} — child joins up+left+right ({@code ┴})</li>
 * <li>{@code cross} — vertical crosses horizontal ({@code ┼})</li>
 * <li>{@code topLeft} — corner: down+right ({@code ┌})</li>
 * <li>{@code topRight} — corner: down+left ({@code ┐})</li>
 * <li>{@code bottomLeft} — corner: up+right ({@code └})</li>
 * <li>{@code bottomRight} — corner: up+left ({@code ┘})</li>
 * <li>{@code rightTee} — T-junction: up+down+right ({@code ├})</li>
 * <li>{@code leftTee} — T-junction: up+down+left ({@code ┤})</li>
 * </ul>
 */
public enum GraphStyle {

    ASCII('-', '|', '+', '+', '+', '+', '+', '+', '+', '+', '+'),
    UNICODE('\u2500', '\u2502', '\u252C', '\u2534', '\u253C',
            '\u250C', '\u2510', '\u2514', '\u2518', '\u251C', '\u2524'),
    ROUNDED('\u2500', '\u2502', '\u252C', '\u2534', '\u253C',
            '\u256D', '\u256E', '\u2570', '\u256F', '\u251C', '\u2524');

    private final char horizontal;
    private final char vertical;
    private final char downTee;
    private final char upTee;
    private final char cross;
    private final char topLeft;
    private final char topRight;
    private final char bottomLeft;
    private final char bottomRight;
    private final char rightTee;
    private final char leftTee;

    GraphStyle(char horizontal, char vertical, char downTee, char upTee, char cross,
            char topLeft, char topRight, char bottomLeft, char bottomRight,
            char rightTee, char leftTee) {
        this.horizontal = horizontal;
        this.vertical = vertical;
        this.downTee = downTee;
        this.upTee = upTee;
        this.cross = cross;
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomLeft = bottomLeft;
        this.bottomRight = bottomRight;
        this.rightTee = rightTee;
        this.leftTee = leftTee;
    }

    public char horizontal() {
        return horizontal;
    }

    public char vertical() {
        return vertical;
    }

    public char downTee() {
        return downTee;
    }

    public char upTee() {
        return upTee;
    }

    public char cross() {
        return cross;
    }

    public char topLeft() {
        return topLeft;
    }

    public char topRight() {
        return topRight;
    }

    public char bottomLeft() {
        return bottomLeft;
    }

    public char bottomRight() {
        return bottomRight;
    }

    public char rightTee() {
        return rightTee;
    }

    public char leftTee() {
        return leftTee;
    }
}
