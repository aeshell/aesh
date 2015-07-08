/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.cl.internal;

import org.jboss.aesh.cl.validator.OptionValidatorException;

import java.lang.reflect.Field;

/**
 * @author Brett Meyer.
 */
public class ProcessedInputPrompt extends CommandFieldValueInjector {

	private final String prompt;

	private final Character mask;

	private final int order;

	private final String fieldName;

	public ProcessedInputPrompt(String prompt, char mask, int order, String fieldName) {
		this.prompt = prompt;
		if (mask == '\u0000') {
			this.mask = null;
		} else {
			this.mask = mask;
		}
		this.order = order;
		this.fieldName = fieldName;
	}

	public String getPrompt() {
		return prompt;
	}

	public Character getMask() {
		return mask;
	}

	public int getOrder() {
		return order;
	}

	@SuppressWarnings("unchecked")
	public void injectValueIntoField(Object instance, String value) throws OptionValidatorException {
		try {
			Field field = getField(instance.getClass(), fieldName, instance);

			// TODO: If we eventually include converters and validators, will need something like ProcessedOption's doConvert.
			field.set(instance, value);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
