/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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

import org.aesh.terminal.utils.ANSI;

/**
 * Represents a single line in a selection list.
 * Tracks focus (cursor position) and selection state,
 * and renders with Unicode indicators and ANSI color when available.
 */
public class SelectLine {

    // Unicode indicators
    private static final String FOCUSED_MARKER = "\u276F "; // ❯
    private static final String UNFOCUSED_MARKER = "  ";
    private static final String SELECTED_ICON = "\u25C9 "; // ◉
    private static final String UNSELECTED_ICON = "\u25EF "; // ◯

    // ASCII fallback (when NO_COLOR is set or terminal doesn't support Unicode)
    private static final String FOCUSED_MARKER_ASCII = "> ";
    private static final String UNFOCUSED_MARKER_ASCII = "  ";
    private static final String SELECTED_ICON_ASCII = "[*] ";
    private static final String UNSELECTED_ICON_ASCII = "[ ] ";

    private static final boolean NO_COLOR = System.getenv("NO_COLOR") != null;

    private final int maxLength;
    private boolean selected;
    private boolean focus;
    private final String description;

    public SelectLine(String description, int maxLength) {
        this(description, false, maxLength);
    }

    public SelectLine(String description, boolean selected, int maxLength) {
        this.description = description;
        this.maxLength = maxLength;
        this.selected = selected;
    }

    public void select() {
        selected = !selected;
    }

    public void setFocus(boolean focus) {
        this.focus = focus;
    }

    /**
     * Toggle focus state. Kept for backward compatibility.
     */
    public void focus() {
        focus = !focus;
    }

    public boolean isFocused() {
        return focus;
    }

    public boolean isSelected() {
        return selected;
    }

    /**
     * Render the line for single-select mode (no checkbox, just focus marker).
     */
    public String printSingleSelect() {
        StringBuilder sb = new StringBuilder();
        if (NO_COLOR) {
            sb.append(focus ? FOCUSED_MARKER_ASCII : UNFOCUSED_MARKER_ASCII);
            sb.append(description);
        } else {
            sb.append(focus ? FOCUSED_MARKER : UNFOCUSED_MARKER);
            if (focus) {
                sb.append(ANSI.BOLD).append(description).append(ANSI.BOLD_OFF);
            } else {
                sb.append(description);
            }
        }
        return truncate(sb.toString());
    }

    /**
     * Render the line for multi-select mode (checkbox + focus marker).
     */
    public String print() {
        StringBuilder sb = new StringBuilder();
        if (NO_COLOR) {
            sb.append(focus ? FOCUSED_MARKER_ASCII : UNFOCUSED_MARKER_ASCII);
            sb.append(selected ? SELECTED_ICON_ASCII : UNSELECTED_ICON_ASCII);
            sb.append(description);
        } else {
            sb.append(focus ? FOCUSED_MARKER : UNFOCUSED_MARKER);
            if (selected) {
                sb.append(ANSI.GREEN_TEXT).append(SELECTED_ICON).append(description).append(ANSI.DEFAULT_TEXT);
            } else {
                sb.append(UNSELECTED_ICON).append(description);
            }
        }
        return truncate(sb.toString());
    }

    public String value() {
        return description;
    }

    private String truncate(String text) {
        // Strip ANSI for length calculation
        String plain = text.replaceAll("\u001B\\[[;\\d]*m", "");
        if (plain.length() > maxLength) {
            // Truncate the plain text and re-apply
            return text.substring(0, Math.min(text.length(), maxLength - 3)) + "...";
        }
        return text;
    }
}
