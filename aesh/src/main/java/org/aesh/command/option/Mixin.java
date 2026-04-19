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
 * Marks a field as a mixin whose annotated options are incorporated
 * into the parent command. This enables composition-based reuse of
 * option groups across commands without requiring class inheritance.
 *
 * <pre>
 * public class LoggingMixin {
 *     &#064;Option(name = "verbose", hasValue = false)
 *     boolean verbose;
 * }
 *
 * &#064;CommandDefinition(name = "run")
 * public class RunCommand implements Command {
 *     &#064;Mixin
 *     LoggingMixin logging;
 * }
 * </pre>
 *
 * @author Aesh team
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Mixin {
}
