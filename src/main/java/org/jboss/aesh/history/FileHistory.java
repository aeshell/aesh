/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.history;

import org.jboss.aesh.console.Config;

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

    private File historyFile;

    public FileHistory(File file, int maxSize) throws IOException {
        super(maxSize);
        historyFile = file;
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
