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
package org.jboss.aesh.cl;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Similar to {@link org.jboss.aesh.cl.Option}, but prompts the user for input at the beginning of command execution
 *
 * @author Brett Meyer
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface InputPrompt {

    /**
     * The prompt text to display.
     */
    String prompt() default "";

    /**
     * If specified, will mask the user's input with the given character.  Example: '*' for use on password inputs.
     */
    char inputMask() default '\u0000';

	/**
	 * If more than one @InputPrompt is used on a command, order their execution with this field.
	 */
	int order() default 0;
}
