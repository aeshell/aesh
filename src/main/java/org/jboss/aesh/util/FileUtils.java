/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.io.FileResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * Helper to find proper files/directories given partial paths/filenames.
 * Should be rewritten as its now just a hack to get it working.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileUtils {

    public static void saveFile(FileResource file, String text, boolean append) throws IOException {
        if(file.isDirectory()) {
            throw new IOException(file+": Is a directory");
        }
        try {
            if(file.isLeaf()) {
                // append text at the end of the file
                if(!append)
                    file.delete();


                file.writeFileResource().write(text.getBytes());
                file.writeFileResource().flush();
            }
            else {
                //create a new file and write to it
                //fileWriter = new FileWriter(file, false);
                file.writeFileResource().write(text.getBytes());
                file.writeFileResource().flush();
            }
        }
        finally {
            if(file != null)
                file.writeFileResource().close();
        }
    }

    public static String readFile(FileResource file) throws IOException {
        if(file.isDirectory()) {
            throw new IOException(file+": Is a directory");
        }
        else if(file.isLeaf()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(file.readFileResource()));
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
            throw new IOException(file+": File unknown");
        }
    }
}
