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
package org.jboss.aesh.history;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.settings.FileAccessPermission;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


/**
 * Read the history file at init and writeToStdOut to it at shutdown
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileHistory extends InMemoryHistory {

    private final File historyFile;
    private final FileAccessPermission historyFilePermission;

    public FileHistory(File file, int maxSize) throws IOException {
        this(file, maxSize, null);
    }

    public FileHistory(File file, int maxSize, FileAccessPermission historyFilePermission) throws IOException {
        super(maxSize);
        historyFile = file;
        this.historyFilePermission = historyFilePermission;
        readFile();
    }

    /**
     * Read specified history file to history buffer
     *
     * @throws IOException io
     */
    private void readFile() throws IOException {
        if(historyFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(historyFile))) {
                String line;
                while((line = reader.readLine()) != null)
                    push(line);
            } catch(FileNotFoundException ignored) {
                //AESH-205
            }
        }
    }

    /**
     * Write the content of the history buffer to file
     *
     * @throws IOException io
     */
    private void writeFile() throws IOException {
        historyFile.delete();
        try (FileWriter fw = new FileWriter(historyFile)) {
            for(int i=0; i < size();i++)
                fw.write(get(i) + (Config.getLineSeparator()));
        }
        if (historyFilePermission != null) {
            historyFile.setReadable(false, false);
            historyFile.setReadable(historyFilePermission.isReadable(), historyFilePermission.isReadableOwnerOnly());
            historyFile.setWritable(false, false);
            historyFile.setWritable(historyFilePermission.isWritable(), historyFilePermission.isWritableOwnerOnly());
            historyFile.setExecutable(false, false);
            historyFile.setExecutable(historyFilePermission.isExecutable(),
                    historyFilePermission.isExecutableOwnerOnly());
        }
    }

    @Override
    public void stop() {
       try {
           writeFile();
       } catch (IOException e) {
           e.printStackTrace();
       }
    }

}
