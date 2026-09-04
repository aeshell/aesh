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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.builtins.less;

import org.aesh.command.man.FileParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Util method that tries to read a file
 * and prepare it to be displayed in a terminal
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class SimpleFileParser implements FileParser {

    private String pageAsString;
    private String fileName;
    private InputStreamReader reader;

    public SimpleFileParser() {
    }

    /**
     * Read from a specified filename. Also supports gzipped files.
     *
     * @param filename File
     * @throws IOException
     */
    public void setFile(String filename) throws IOException {
        setFile(new File(filename));
    }

    /**
     * Read from a specified file. Also supports gzipped files.
     *
     * @param file File
     * @throws IOException
     */
    public void setFile(File file) throws IOException {
        if(!file.isFile())
            throw new IllegalArgumentException(file+" must be a file.");
        else {
            if(file.getName().endsWith("gz"))
                initGzReader(file);
            else
                initReader(file);
        }
    }

    public void setFile(InputStream inputStream) {
        reader = new InputStreamReader(inputStream);
    }

    public void setFile(InputStream inputStream, String fileName) {
        reader = new InputStreamReader(inputStream);
        this.fileName = fileName;
    }


    /**
     * Read a file resouce located in a jar
     *
     * @param fileName name
     */
    public void setFileFromAJar(String fileName) {
        InputStream is = this.getClass().getResourceAsStream(fileName);
        if(is != null) {
            this.fileName = fileName;
            reader = new InputStreamReader(is);
        }
    }

    private void initReader(File file) throws FileNotFoundException {
        fileName = file.getAbsolutePath();
        reader = new FileReader(file);
    }

    public void readPageAsString(String pageAsString) {
        this.pageAsString = pageAsString;
    }

    @Override
    public String getName() {
        if(fileName != null)
            return fileName;
        else
            return "STREAM";
    }

    private void initGzReader(File file) throws IOException {
        fileName = file.getAbsolutePath();
        GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(file));
        reader = new InputStreamReader(gzip);
    }

    @Override
    public List<String> loadPage(int columns) throws IOException {
        List<String> lines = new ArrayList<String>();
        //read file and save each line in a list
        if(reader != null) {
            BufferedReader br = new BufferedReader(reader);
            try {
                String line = br.readLine();

                while (line != null) {
                    if(line.length() > columns) {
                        //split the line in the size of column
                        for(String s : line.split("(?<=\\G.{"+columns+"})"))
                            lines.add(s);
                    }
                    else
                        lines.add(line);
                    line = br.readLine();
                }
            }
            finally {
                br.close();
            }
        }
        else if(pageAsString != null) {
            for(String s : pageAsString.split("\n")) {
                for(String s2 : s.split("(?<=\\G.{" + columns + "})"))
                    lines.add(s2);
            }
        }

        return lines;
    }

}
