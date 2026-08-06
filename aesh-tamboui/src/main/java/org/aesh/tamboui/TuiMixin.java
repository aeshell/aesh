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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.tamboui;

import java.time.Duration;

import org.aesh.command.option.Option;

import dev.tamboui.tui.TuiConfig;

/**
 * Aesh mixin that provides common TUI-related command line options.
 * <p>
 * Use this mixin to add TUI options to your commands. The options are applied
 * automatically when used with {@link TuiCommand} or {@link TuiAppCommand},
 * or manually via {@link #applyTo(TuiConfig.Builder)}.
 *
 * <pre>
 * {@code
 * &#64;CommandDefinition(name = "dashboard", description = "Show dashboard")
 * public class DashboardCommand extends TuiCommand {
 *     &#64;Mixin
 *     TuiMixin tuiOptions;
 *
 *     @Override
 *     protected void runTui(TuiRunner runner, CommandInvocation inv) throws Exception {
 *         runner.run(...);
 *     }
 * }
 * }
 * </pre>
 *
 * @see TuiCommand
 * @see TuiAppCommand
 */
public class TuiMixin {

    @Option(name = "no-alt-screen", hasValue = false, negatable = true, description = "Disable alternate screen mode")
    boolean noAltScreen;

    @Option(name = "show-cursor", hasValue = false, description = "Show cursor in TUI mode")
    boolean showCursor;

    @Option(name = "mouse", hasValue = false, description = "Enable mouse capture")
    boolean mouseCapture;

    @Option(name = "tick-rate", defaultValue = "0", description = "Tick rate in milliseconds for animations (0 to disable)")
    int tickRateMs;

    @Option(name = "poll-timeout", defaultValue = "100", description = "Event poll timeout in milliseconds")
    int pollTimeoutMs;

    /**
     * Apply the parsed TUI options to a TuiConfig builder.
     *
     * @param builder the config builder to configure
     * @return the builder for chaining
     */
    public TuiConfig.Builder applyTo(TuiConfig.Builder builder) {
        builder.alternateScreen(!noAltScreen)
                .hideCursor(!showCursor)
                .mouseCapture(mouseCapture)
                .pollTimeout(Duration.ofMillis(pollTimeoutMs));

        if (tickRateMs > 0) {
            builder.tickRate(Duration.ofMillis(tickRateMs));
        }

        return builder;
    }

    public boolean isNoAltScreen() {
        return noAltScreen;
    }

    public boolean isShowCursor() {
        return showCursor;
    }

    public boolean isMouseCapture() {
        return mouseCapture;
    }

    public int getTickRateMs() {
        return tickRateMs;
    }

    public int getPollTimeoutMs() {
        return pollTimeoutMs;
    }
}
