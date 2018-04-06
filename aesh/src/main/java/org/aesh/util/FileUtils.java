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
package org.aesh.util;

import org.aesh.io.Resource;

import java.io.IOException;
import java.io.OutputStream;


/**
 * Helper to find proper files/directories given partial paths/filenames.
 * Should be rewritten as its now just a hack to get it working.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileUtils {

    public static void saveFile(Resource file, String text, boolean append) throws IOException {
        if(file.isDirectory()) {
            throw new IOException(file+": Is a directory");
        }

        try (OutputStream out = file.write(append);) {
            out.write(text.getBytes());
        }
    }
}
