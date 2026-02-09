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
package org.aesh.command.option;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a field to receive the parent command instance when executing
 * as a subcommand. The field type must match the parent command class.
 *
 * <p>
 * This annotation enables subcommands to access the parsed options
 * and arguments of their parent group command. It works both in
 * sub-command mode (interactive) and when commands are invoked directly
 * (e.g., "parent --option value sub --suboption").
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * {@literal @}GroupCommandDefinition(name = "module", groupCommands = {TagCommand.class})
 * public class ModuleCommand implements Command&lt;CommandInvocation&gt; {
 *     {@literal @}Option(name = "verbose", hasValue = false)
 *     private boolean verbose;
 *
 *     {@literal @}Argument
 *     private String moduleName;
 *     // ...
 * }
 *
 * {@literal @}CommandDefinition(name = "tag", description = "Manage tags")
 * public class TagCommand implements Command&lt;CommandInvocation&gt; {
 *
 *     {@literal @}ParentCommand
 *     private ModuleCommand parent;
 *
 *     {@literal @}Argument
 *     private String tagName;
 *
 *     public CommandResult execute(CommandInvocation invocation) {
 *         // Access parent's parsed values
 *         String module = parent.getModuleName();
 *         boolean verbose = parent.isVerbose();
 *         // ...
 *     }
 * }
 * </pre>
 *
 * @author Aesh team
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface ParentCommand {
    // No properties needed - the field type determines which parent to inject
}
