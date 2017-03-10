/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017 Red Hat Inc. and/or its affiliates and other contributors
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
package org.aesh.command.impl.operator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import org.aesh.console.AeshContext;
import org.aesh.util.Parser;

/**
 *
 * @author jdenise@redhat.com
 */
public abstract class OutputDelegate {

    private BufferedWriter writer;
    private final File outputFile;
    protected OutputDelegate(AeshContext context, String file) {
        Objects.requireNonNull(file);
        File f = new File(file);
        if (!f.isAbsolute()) {
            f = new File(context.getCurrentWorkingDirectory().getAbsolutePath(), file);
        }
        outputFile = f;
    }

    protected abstract BufferedWriter buildWriter(File f) throws IOException;

    public void write(String msg) throws IOException {
        msg = Parser.stripAwayAnsiCodes(msg);
        if (writer == null) {
            writer = buildWriter(outputFile);
        }
        writer.append(msg);
        writer.flush();
    }
}
