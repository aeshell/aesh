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
import java.io.IOException;
import org.aesh.terminal.utils.Parser;

/**
 *
 * @author jdenise@redhat.com
 */
public abstract class OutputDelegate {

    private BufferedWriter writer;
    private IOException exception;

    protected OutputDelegate() {
    }

    protected abstract BufferedWriter buildWriter() throws IOException;

    public void write(String msg) {
        try {
            msg = Parser.stripAwayAnsiCodes(msg);
            if (writer == null && exception == null) {
                writer = buildWriter();
            }
            //if we have a writer, write
            if(writer != null) {
                writer.append(msg);
                writer.flush();
            }
        }
        catch (IOException e) {
            exception = e;
        }
    }

    public void close() throws IOException {
        //if(writer != null)
        //    writer.close();
        if(exception != null)
            throw exception;
    }

}
