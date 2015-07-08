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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author Brett Meyer.
 */
public abstract class CommandFieldValueInjector {

	protected Field getField(Class clazz, String fieldName, Object instance)
			throws NoSuchFieldException, NoSuchMethodException {
		try {
			Field field = clazz.getDeclaredField(fieldName);

			if(!Modifier.isPublic(field.getModifiers()))
				field.setAccessible(true);
			if(!Modifier.isPublic(instance.getClass().getModifiers())) {
				Constructor constructor = instance.getClass().getDeclaredConstructor();
				if(constructor != null)
					constructor.setAccessible(true);
			}

			return field;
		}
		catch(NoSuchFieldException nsfe) {
			if(clazz.getSuperclass() != null)
				return getField(clazz.getSuperclass(), fieldName, instance);
			else throw nsfe;
		}
	}
}
