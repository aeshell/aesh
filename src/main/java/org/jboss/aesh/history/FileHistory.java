/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.history;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.util.LoggerUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;


/**
 * Read the history file at init and writeToStdOut to it at shutdown
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileHistory extends InMemoryHistory {

    private String historyFile;
    private static final Logger LOGGER = LoggerUtil.getLogger(FileHistory.class.getName());

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
    private void readFile() {
        if(new File(historyFile).exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(historyFile));

                String line;
                while((line = reader.readLine()) != null)
                    push(line);

                reader.close();
            }
            catch (IOException e) {
                LOGGER.warning("Failed to write to history file; "+e.getMessage());
            }
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
