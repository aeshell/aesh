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
package org.aesh.command;

/**
 * Tells a {@link HelpSectionProvider} which command is being rendered, so one
 * provider class can serve many commands (e.g. loading prose from a resource
 * bundle keyed by command path) instead of needing a subclass per command.
 * <p>
 * Passed to the {@code HelpContext} overloads of {@code HelpSectionProvider};
 * the default implementations ignore it and delegate to the older methods,
 * so existing providers keep working unchanged.
 */
public final class HelpContext {

    private final Command<?> command;
    private final String name;
    private final String fullName;
    private final DocFormat format;

    /**
     * @param command the command being rendered, or null when rendering
     *        without an instance (e.g. metadata-only listings)
     * @param name the simple command name
     * @param fullName the space-separated path from the root command
     *        (e.g. {@code "git remote add"}), for a top-level command
     *        this is the name itself
     * @param format the output format, or null for terminal {@code --help}
     */
    public HelpContext(Command<?> command, String name, String fullName, DocFormat format) {
        this.command = command;
        this.name = name;
        this.fullName = fullName;
        this.format = format;
    }

    /**
     * @return the command being rendered, or null when there is none
     */
    public Command<?> command() {
        return command;
    }

    /**
     * @return the simple command name
     */
    public String name() {
        return name;
    }

    /**
     * @return the space-separated path from the root command
     */
    public String fullName() {
        return fullName;
    }

    /**
     * @return the output format, or null for terminal {@code --help}
     */
    public DocFormat format() {
        return format;
    }
}
