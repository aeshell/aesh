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

import org.aesh.command.shell.Shell;
import org.aesh.terminal.utils.ANSI;

/**
 * A pull-based progress bar utility for displaying progress of long-running
 * operations in the terminal.
 *
 * <p>Usage example:</p>
 * <pre>
 * ProgressBar progress = ProgressBar.builder()
 *         .shell(invocation.getShell())
 *         .total(fileCount)
 *         .label("Processing")
 *         .style(ProgressBarStyle.UNICODE)
 *         .showPercentage(true)
 *         .showRatio(true)
 *         .build();
 *
 * for (File file : files) {
 *     processFile(file);
 *     progress.step();
 * }
 * progress.complete();
 * </pre>
 */
public class ProgressBar {

    private static final int MIN_BAR_WIDTH = 10;
    private static final int DEFAULT_WIDTH = 80;

    private final Shell shell;
    private final long total;
    private final String label;
    private final ProgressBarStyle style;
    private final boolean showPercentage;
    private final boolean showRatio;
    private final int width;

    private long current;

    private ProgressBar(Builder builder) {
        this.shell = builder.shell;
        this.total = builder.total;
        this.label = builder.label;
        this.style = builder.style;
        this.showPercentage = builder.showPercentage;
        this.showRatio = builder.showRatio;
        this.current = 0;

        if (builder.width > 0) {
            this.width = builder.width;
        } else if (shell != null && shell.size() != null) {
            this.width = shell.size().getWidth();
        } else {
            this.width = DEFAULT_WIDTH;
        }
    }

    /**
     * Set absolute progress value.
     *
     * @param value the current progress value
     */
    public void update(long value) {
        this.current = value;
        display();
    }

    /**
     * Increment progress by 1.
     */
    public void step() {
        step(1);
    }

    /**
     * Increment progress by a given amount.
     *
     * @param n the amount to increment by
     */
    public void step(long n) {
        this.current += n;
        display();
    }

    /**
     * Mark the progress as complete. Fills to 100% and prints a newline
     * so subsequent output appears below the bar.
     */
    public void complete() {
        this.current = total;
        display();
        if (shell != null) {
            shell.writeln("");
        }
    }

    /**
     * Mark the progress as complete with a message replacing the bar.
     *
     * @param message the completion message to display
     */
    public void complete(String message) {
        if (shell != null) {
            shell.write(ANSI.CURSOR_START);
            shell.write(ANSI.ERASE_WHOLE_LINE);
            shell.writeln(message);
        }
    }

    private void display() {
        if (shell == null) {
            return;
        }
        shell.write(ANSI.CURSOR_START);
        shell.write(ANSI.ERASE_WHOLE_LINE);
        shell.write(render(current, total, width));
    }

    /**
     * Render the progress bar as a string. Package-private for testing.
     *
     * @param current the current progress value
     * @param total the total value
     * @param termWidth the available terminal width
     * @return the rendered progress bar string
     */
    String render(long current, long total, int termWidth) {
        // Clamp current to [0, total]
        if (current < 0) {
            current = 0;
        }
        if (total > 0 && current > total) {
            current = total;
        }

        // Calculate percentage
        int percent;
        if (total <= 0) {
            percent = 100;
        } else {
            percent = (int) (current * 100 / total);
        }

        // Build suffix parts
        StringBuilder suffix = new StringBuilder();
        if (showPercentage) {
            suffix.append(' ');
            if (percent < 100) {
                suffix.append(' ');
            }
            if (percent < 10) {
                suffix.append(' ');
            }
            suffix.append(percent).append('%');
        }
        if (showRatio) {
            suffix.append(" (").append(current).append('/').append(total).append(')');
        }

        // Build prefix
        StringBuilder prefix = new StringBuilder();
        if (label != null && !label.isEmpty()) {
            prefix.append(label).append(' ');
        }

        // Brackets take 2 chars
        int fixedWidth = prefix.length() + 2 + suffix.length();
        int barWidth = termWidth - fixedWidth;
        if (barWidth < MIN_BAR_WIDTH) {
            barWidth = MIN_BAR_WIDTH;
        }

        // Calculate fill
        int fillCount;
        if (total <= 0) {
            fillCount = barWidth;
        } else {
            fillCount = (int) (barWidth * current / total);
        }
        if (fillCount > barWidth) {
            fillCount = barWidth;
        }
        int emptyCount = barWidth - fillCount;

        // Build the bar
        StringBuilder sb = new StringBuilder(termWidth);
        sb.append(prefix);
        sb.append(style.leftBracket());

        if (style == ProgressBarStyle.ARROW && fillCount > 0 && fillCount < barWidth) {
            // Fill with = except last which is >
            for (int i = 0; i < fillCount - 1; i++) {
                sb.append(style.fillChar());
            }
            sb.append(style.tipChar());
        } else {
            for (int i = 0; i < fillCount; i++) {
                sb.append(style.fillChar());
            }
        }

        for (int i = 0; i < emptyCount; i++) {
            sb.append(style.emptyChar());
        }

        sb.append(style.rightBracket());
        sb.append(suffix);

        return sb.toString();
    }

    /**
     * Creates a new builder for constructing a ProgressBar.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link ProgressBar} instances.
     */
    public static class Builder {
        private Shell shell;
        private long total;
        private String label;
        private ProgressBarStyle style = ProgressBarStyle.ASCII;
        private boolean showPercentage = true;
        private boolean showRatio = false;
        private int width = -1;

        private Builder() {
        }

        /**
         * Set the shell to write output to.
         *
         * @param shell the Shell instance
         * @return this builder
         */
        public Builder shell(Shell shell) {
            this.shell = shell;
            return this;
        }

        /**
         * Set the total number of steps.
         *
         * @param total the total value
         * @return this builder
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Set an optional label displayed before the bar.
         *
         * @param label the label text
         * @return this builder
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * Set the visual style of the progress bar.
         *
         * @param style the ProgressBarStyle
         * @return this builder
         */
        public Builder style(ProgressBarStyle style) {
            this.style = style;
            return this;
        }

        /**
         * Whether to show percentage (default true).
         *
         * @param showPercentage true to show percentage
         * @return this builder
         */
        public Builder showPercentage(boolean showPercentage) {
            this.showPercentage = showPercentage;
            return this;
        }

        /**
         * Whether to show current/total ratio (default false).
         *
         * @param showRatio true to show ratio
         * @return this builder
         */
        public Builder showRatio(boolean showRatio) {
            this.showRatio = showRatio;
            return this;
        }

        /**
         * Set an explicit bar width in columns. If not set, the width is
         * auto-calculated from the terminal size or defaults to 80.
         *
         * @param width the width in columns
         * @return this builder
         */
        public Builder width(int width) {
            this.width = width;
            return this;
        }

        /**
         * Build the ProgressBar instance.
         *
         * @return a new ProgressBar
         */
        public ProgressBar build() {
            return new ProgressBar(this);
        }
    }
}
