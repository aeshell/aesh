/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

        if(append && file.isLeaf()) {
            //find method to append the text to the file
        }

        OutputStream out = file.write();

        out.write(text.getBytes());
        out.flush();
        out.close();

    }

    public static String readFile(Resource file) throws IOException {
        if(file.isDirectory()) {
            throw new IOException(file+": Is a directory");
        }
        else if(file.isLeaf()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(file.read()));
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
