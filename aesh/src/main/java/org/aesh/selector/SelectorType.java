/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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

/**
 * Types of interactive selector prompts available for {@code @Option} fields.
 * <p>
 * When set on an option annotation (e.g., {@code @Option(selector = SelectorType.SELECT)}),
 * the selector UI is shown during command execution if the user did not provide
 * the option value on the command line. The selected value(s) are injected into
 * the field before the command's {@code execute()} method runs.
 */
public enum SelectorType {
    /** Free text input (uses {@code shell.readLine()}). */
    INPUT,
    /** Masked password input. */
    PASSWORD,
    /** Single-select list with arrow-key navigation. */
    SELECT,
    /** Multi-select checkboxes with Space to toggle. */
    MULTI_SELECT,
    /** Yes/no confirmation prompt. For boolean fields. */
    CONFIRM,
    /** Key-based expandable choice with help text. */
    EXPAND,
    /** No selector — default behavior. */
    NO_OP,
    /**
     * @deprecated Use {@link #MULTI_SELECT} instead.
     */
    @Deprecated
    SELECTIONS
}
