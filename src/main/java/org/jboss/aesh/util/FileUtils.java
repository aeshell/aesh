/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.settings.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Helper to find proper files/directories given partial paths/filenames.
 * Should be rewritten as its now just a hack to get it working.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileUtils {

    private static Logger logger = LoggerUtil.getLogger(FileUtils.class.getName());

    public static void saveFile(File file, String text, boolean append) throws IOException {
        if(file.isDirectory()) {
            if(Settings.getInstance().isLogging())
                logger.info("Cannot save file "+file+", it is a directory");
            throw new IOException(file+": Is a directory");
        }
        else if(file.isFile()) {
            FileWriter fileWriter;
            // append text at the end of the file
            if(append)
                fileWriter = new FileWriter(file, true);
             //overwrite the file
            else
                fileWriter = new FileWriter(file, false);

            fileWriter.write(text);
            fileWriter.flush();
            fileWriter.close();
        }
        else {
            //create a new file and write to it
            FileWriter fileWriter = new FileWriter(file, false);
            fileWriter.write(text);
            fileWriter.flush();
            fileWriter.close();
        }
    }

    public static String readFile(File file) throws IOException {
        if(file.isDirectory()) {
            if(Settings.getInstance().isLogging())
                logger.info("Cannot save file "+file+", it is a directory");
            throw new IOException(file+": Is a directory");
        }
        else if(file.isFile()) {
            BufferedReader br = new BufferedReader(new FileReader(file));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line).append(Config.getLineSeparator());
                    line = br.readLine();
                }
                return sb.toString();
            }
            finally {
                br.close();
            }
        }
        else {
            if(Settings.getInstance().isLogging())
                logger.info("Cannot read file "+file+", file unknown");
            throw new IOException(file+": File unknown");
        }
    }
}
