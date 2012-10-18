/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Helper to find proper files/directories given partial paths/filenames.
 * Should be rewritten as its now just a hack to get it working.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileUtils {

    private static final Pattern startsWithParent = Pattern.compile("^\\.\\..*");
    private static final Pattern containParent = Config.isOSPOSIXCompatible() ?
            Pattern.compile("[\\.\\.["+ Config.getPathSeparator()+"]?]+") : Pattern.compile("[\\.\\.[\\\\]?]+");
    private static final Pattern space = Pattern.compile(".+\\s+.+");
    private static final Pattern startsWithSlash = Config.isOSPOSIXCompatible() ?
            Pattern.compile("^\\"+Config.getPathSeparator()+".*") : Pattern.compile("^\\\\.*");
    private static final Pattern endsWithSlash = Config.isOSPOSIXCompatible() ?
            Pattern.compile(".*\\"+Config.getPathSeparator()+"$") : Pattern.compile(".*\\\\$");

    public static void listMatchingDirectories(CompleteOperation completion, String possibleDir, File cwd) {
        // that starts with possibleDir
        List<String> returnFiles = new ArrayList<String>();
        if (possibleDir.trim().isEmpty()) {
            List<String> allFiles = listDirectory(cwd);
            for (String file : allFiles)
                if (file.startsWith(possibleDir))
                    returnFiles.add(Parser.switchSpacesToEscapedSpacesInWord( file.substring(possibleDir.length())));

            completion.addCompletionCandidates(returnFiles);
        }
        else if (!startsWithSlash.matcher(possibleDir).matches() &&
                new File(cwd.getAbsolutePath() +
                        Config.getPathSeparator() +possibleDir).isDirectory()) {
            if(!endsWithSlash.matcher(possibleDir).matches()){
                returnFiles.add(Config.getPathSeparator());
                completion.addCompletionCandidates(returnFiles);
            }
            else {
                completion.addCompletionCandidates( listDirectory(new File(cwd.getAbsolutePath() +
                        Config.getPathSeparator() +possibleDir)));
            }
        }
        else if(new File(cwd.getAbsolutePath() +Config.getPathSeparator()+ possibleDir).isFile()) {
            returnFiles.add(" ");
            completion.addCompletionCandidates(returnFiles);
        }
        //else if(possibleDir.startsWith(("/")) && new File(possibleDir).isFile()) {
        else if(startsWithSlash.matcher(possibleDir).matches() &&
                new File(possibleDir).isFile()) {
            returnFiles.add(" ");
            completion.addCompletionCandidates(returnFiles);
        }
        else {
            returnFiles = new ArrayList<String>();
            if(new File(possibleDir).isDirectory() &&
                    !endsWithSlash.matcher(possibleDir).matches()) {
                returnFiles.add(Config.getPathSeparator());
                completion.addCompletionCandidates(returnFiles);
                return;
            }
            else if(new File(possibleDir).isDirectory() &&
                    !endsWithSlash.matcher(possibleDir).matches()) {
                completion.addCompletionCandidates( listDirectory(new File(possibleDir)));
                return;
            }


            //1.list possibleDir.substring(pos
            String lastDir = null;
            String rest = null;
            if(possibleDir.contains(Config.getPathSeparator())) {
                lastDir = possibleDir.substring(0,possibleDir.lastIndexOf(Config.getPathSeparator()));
                rest = possibleDir.substring(possibleDir.lastIndexOf(Config.getPathSeparator())+1);
            }
            else {
                if(new File(cwd+Config.getPathSeparator()+possibleDir).exists())
                    lastDir = possibleDir;
                else {
                    rest = possibleDir;
                }
            }

            List<String> allFiles;
            if(startsWithSlash.matcher(possibleDir).matches()) {
                if(lastDir.startsWith(Config.getPathSeparator()))
                    allFiles =  listDirectory(new File(lastDir));
                else
                    allFiles =  listDirectory(new File(Config.getPathSeparator()+lastDir));
            }
            else if(lastDir != null)
                allFiles =  listDirectory(new File(cwd+
                        Config.getPathSeparator()+lastDir));
            else
                allFiles =  listDirectory(cwd);

            //TODO: optimize
            //1. remove those that do not start with rest, if its more than one
            if(rest != null && !rest.isEmpty()) {
                for (String file : allFiles)
                    if (file.startsWith(rest))
                        //returnFiles.add(file);
                        returnFiles.add(Parser.switchSpacesToEscapedSpacesInWord( file.substring(rest.length())));
            }
            else {
                for(String file : allFiles)
                    returnFiles.add(Parser.switchSpacesToEscapedSpacesInWord(file));
            }

            if(returnFiles.size() > 1) {
                String startsWith = Parser.findStartsWith(returnFiles);
                if(startsWith != null && startsWith.length() > 0) {
                    returnFiles.clear();
                    returnFiles.add(Parser.switchSpacesToEscapedSpacesInWord( startsWith));
                }
                //need to list complete filenames
                else {
                    returnFiles.clear();
                    for (String file : allFiles)
                        if (file.startsWith(rest))
                            returnFiles.add(Parser.switchSpacesToEscapedSpacesInWord( file));
                }
            }

            completion.addCompletionCandidates(returnFiles);
            if(returnFiles.size() > 1 && rest != null && rest.length() > 0)
                completion.setOffset(completion.getCursor()-rest.length());

        }
    }

    private static List<String> listDirectory(File path) {
        List<String> fileNames = new ArrayList<String>();
        if(path != null && path.isDirectory())
            for(File file : path.listFiles())
                fileNames.add(file.getName());

        return fileNames;
    }

    public static String getDirectoryName(File path, File home) {
        if(path.getAbsolutePath().startsWith(home.getAbsolutePath()))
            return "~"+path.getAbsolutePath().substring(home.getAbsolutePath().length());
        else
            return path.getAbsolutePath();
    }

    /**
     * Parse file name
     * 1. .. = parent dir
     * 2. ~ = home dir
     *
     *
     * @param name file
     * @param cwd current working directory
     * @return file correct file
     */
    public static File getFile(String name, String cwd) {
        //contains ..
        if(containParent.matcher(name).matches()) {
            if(startsWithParent.matcher(name).matches()) {

            }

        }
        else if(name.startsWith("~")) {

        }
        else
            return new File(name);

        return null;
    }

    public static void saveFile(File file, String text, boolean append) throws IOException {
        if(file.isDirectory()) {
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
        if(file.isDirectory())
            throw new IOException(file+": Is a directory");
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
            throw new IOException(file+": File unknown");
        }
    }
}
