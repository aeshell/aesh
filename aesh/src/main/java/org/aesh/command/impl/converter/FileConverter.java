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
package org.aesh.command.impl.converter;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;

import java.io.File;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileConverter implements Converter<File, ConverterInvocation> {

    @Override
    public File convert(ConverterInvocation input) {
        return new File(translatePath(input.getAeshContext().getCurrentWorkingDirectory().getAbsolutePath(), input.getInput()));
    }

    public static String translatePath(String cwd, String path) {
        String translated;
        // special character: ~ maps to the user's home directory
        if (path.startsWith("~" + File.separator)) {
            translated = System.getProperty("user.home") + path.substring(1);
        } else if (path.startsWith("~")) {
            String userName = path.substring(1);
            translated = new File(new File(System.getProperty("user.home")).getParent(),
                    userName).getAbsolutePath();
            // Keep the path separator in translated or add one if no user home specified
            translated = userName.isEmpty() || path.endsWith(File.separator) ? translated + File.separator : translated;
        } else if (!new File(path).isAbsolute()) {
            translated = cwd + File.separator + path;
        } else {
            translated = path;
        }
        return translated;
    }

}
