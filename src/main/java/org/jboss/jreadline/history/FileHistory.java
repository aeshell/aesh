/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.history;

import org.jboss.jreadline.console.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


/**
 * Read the history file at init and writeToStdOut to it at shutdown
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileHistory extends InMemoryHistory {

    private String historyFile;

    public FileHistory(String fileName, int maxSize) throws IOException {
        super(maxSize);
        historyFile = fileName;

        readFile();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void start() {
                try {
                    writeFile();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Read specified history file to history buffer
     *
     * @throws IOException io
     */
    private void readFile() throws IOException {

        if(new File(historyFile).exists()) {

            BufferedReader reader =
                    new BufferedReader(new FileReader(historyFile));

            String line;
            while((line = reader.readLine()) != null)
                push(line);

            reader.close();
        }
    }

    /**
     * Write the content of the history buffer to file
     *
     * @throws IOException io
     */
    private void writeFile() throws IOException {
        new File(historyFile).delete();

        FileWriter fw = new FileWriter(historyFile);

        for(int i=0; i < size();i++)
            fw.write(get(i) + (Config.getLineSeparator()));

        fw.flush();
        fw.close();
    }

}
