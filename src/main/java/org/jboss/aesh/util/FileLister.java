/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.parser.Parser;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Helper class to list possible files during a complete operation.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileLister {
    private static final Pattern startsWithParent = Pattern.compile("^\\.\\..*");
    private static final Pattern startsWithHomePattern = Pattern.compile("^~/*");
    private static final Pattern containParent = Config.isOSPOSIXCompatible() ?
            Pattern.compile("[\\.\\.["+ Config.getPathSeparator()+"]?]+") : Pattern.compile("[\\.\\.[\\\\]?]+");
    private static final Pattern space = Pattern.compile(".+\\s+.+");
    private static final Pattern startsWithSlash = Config.isOSPOSIXCompatible() ?
            Pattern.compile("^\\"+Config.getPathSeparator()+".*") : Pattern.compile("^\\\\.*");
    private static final Pattern endsWithSlash = Config.isOSPOSIXCompatible() ?
            Pattern.compile(".*\\"+Config.getPathSeparator()+"$") : Pattern.compile(".*\\\\$");

    private String incDir;
    private File cwd;
    private String rest;
    private String lastDir;
    private FileFilter fileFilter;

    public FileLister(String incDir, File cwd) {
        if(incDir == null)
            throw new IllegalArgumentException("Incoming directory cannot be null");
        if(cwd == null)
            throw new IllegalArgumentException("Current working directory cannot be null");
        this.incDir = Parser.switchEscapedSpacesToSpacesInWord(incDir);
        this.cwd = cwd;
        findRestAndLastDir();
        setFileFilter(Filter.ALL);
    }

    public FileLister(String incDir, File cwd, Filter filter) {
        this(incDir, cwd);
        setFileFilter(filter);
    }

    public FileLister(String incDir, File cwd, FileFilter fileFilter) {
        this(incDir, cwd);
        this.fileFilter = fileFilter;
    }

    private void setFileFilter(Filter filter) {
        if(filter == Filter.ALL)
            fileFilter = new FileAndDirectoryFilter();
        else if(filter == Filter.FILE)
            fileFilter = new OnlyFileFilter();
        else
            fileFilter = new DirectoryFileFilter();
    }

    /**
     * findMatchingDirectories will try to populate the CompleteOperation
     * object based on it initial params.
     *
     * @param completion
     */
    public void findMatchingDirectories(CompleteOperation completion) {
       completion.doAppendSeparator(false);

        //if incDir is empty, just list cwd
        if(incDir.trim().isEmpty()) {
            completion.addCompletionCandidates( listDirectory(cwd, null));
        }
        else if(startWithHome()) {
            if(isHomeAndIncDirADirectory()) {
                if(endWithSlash()) {
                    completion.addCompletionCandidates(
                            listDirectory(new File(Config.getHomeDir()+incDir.substring(1)), null));
                }
                else
                    completion.addCompletionCandidate(Config.getPathSeparator());
            }
            else if(isHomeAndIncDirAFile()) {
                completion.addCompletionCandidate("");
                //append when we have a file
                completion.doAppendSeparator(true);
            }
            //not a directory or file, list what we find
            else {
               listPossibleDirectories(completion);
            }
        }
        else if(!startWithSlash()) {
            if(isCwdAndIncDirADirectory()) {
                if(endWithSlash()) {
                    completion.addCompletionCandidates(
                            listDirectory(new File(cwd.getAbsolutePath() +
                                    Config.getPathSeparator()+incDir), null));
                }
                else
                    completion.addCompletionCandidate(Config.getPathSeparator());
            }
            else if(isCwdAndIncDirAFile()) {
                completion.addCompletionCandidate("");
                //append when we have a file
                completion.doAppendSeparator(true);
            }
            //not a directory or file, list what we find
            else {
               listPossibleDirectories(completion);
            }
        }
        else if(startWithSlash()) {
            if(isIncDirADirectory()) {
                if(endWithSlash()) {
                    completion.addCompletionCandidates(
                            listDirectory(new File(incDir), null));
                }
                else
                    completion.addCompletionCandidate(Config.getPathSeparator());
            }
            else if(isIncDirAFile()) {
                completion.addCompletionCandidate("");
                //append when we have a file
                completion.doAppendSeparator(true);
            }
            //not a directory or file, list what we find
            else {
               listPossibleDirectories(completion);
            }
        }

    }

    private void listPossibleDirectories(CompleteOperation completion) {
        List<String> returnFiles;

        if(startWithSlash()) {
            if(lastDir != null && lastDir.startsWith(Config.getPathSeparator()))
                returnFiles =  listDirectory(new File(lastDir), rest);
            else
                returnFiles =  listDirectory(new File(Config.getPathSeparator()+lastDir), rest);
        }
        else if(startWithHome()) {
            //if(lastDir != null && lastDir.startsWith(Config.getPathSeparator()))
            if(lastDir != null) {
                returnFiles =  listDirectory(new File(Config.getHomeDir()+lastDir.substring(1)), rest);
            }
            else
                returnFiles =  listDirectory(new File(Config.getHomeDir()+Config.getPathSeparator()), rest);
        }
        else if(lastDir != null) {
            returnFiles =  listDirectory(new File(cwd+
                    Config.getPathSeparator()+lastDir), rest);
        }
        else
            returnFiles =  listDirectory(cwd, rest);

        //try to find if more than one filename start with the same word
        if(returnFiles.size() > 1) {
            String startsWith = Parser.findStartsWith(returnFiles);
            if(startsWith.contains(" "))
                startsWith = Parser.switchEscapedSpacesToSpacesInWord(startsWith);
            if(startsWith != null && startsWith.length() > 0 &&
                    startsWith.length() > rest.length()) {
                completion.addCompletionCandidate(Parser.switchSpacesToEscapedSpacesInWord( startsWith.substring(rest.length())));
            }
            //need to list complete filenames
            else {
                completion.addCompletionCandidates(returnFiles);
            }
        }
        else if(returnFiles.size() == 1) {
            if(rest.contains(" "))
                completion.addCompletionCandidate(returnFiles.get(0).substring(Parser.switchSpacesToEscapedSpacesInWord(rest).length()));
            else
                completion.addCompletionCandidate(returnFiles.get(0).substring(rest.length()));
        }

        if(completion.getCompletionCandidates().size() > 1 && rest != null && rest.length() > 0)
            completion.setOffset(completion.getCursor()-rest.length());
    }

    private void findRestAndLastDir() {
        if(incDir.contains(Config.getPathSeparator())) {
            lastDir = incDir.substring(0, incDir.lastIndexOf(Config.getPathSeparator()));
            rest = incDir.substring(incDir.lastIndexOf(Config.getPathSeparator())+1);
        }
        else {
            if(new File(cwd+Config.getPathSeparator()+incDir).exists())
                lastDir = incDir;
            else {
                rest = incDir;
            }
        }
    }

    private boolean isIncDirADirectory() {
        return new File(incDir).isDirectory();
    }

    private boolean isIncDirAFile() {
        return new File(incDir).isFile();
    }

    private boolean isCwdAndIncDirADirectory() {
        return new File(cwd.getAbsolutePath() +
                Config.getPathSeparator() + incDir).isDirectory();
    }

    private boolean isCwdAndIncDirAFile() {
        return new File(cwd.getAbsolutePath() +
                Config.getPathSeparator() + incDir).isFile();
    }

    private boolean isHomeAndIncDirADirectory() {
        return new File(Config.getHomeDir()+
                 incDir.substring(1)).isDirectory();
    }

    private boolean isHomeAndIncDirAFile() {
        return new File(Config.getHomeDir()+
                incDir.substring(1)).isFile();
    }

    private boolean startWithParent() {
        return startsWithParent.matcher(incDir).matches();
    }

    private boolean startWithHome() {
        return incDir.startsWith("~/");
    }

    private boolean startWithSlash() {
        return startsWithSlash.matcher(incDir).matches();
    }

    private boolean endWithSlash() {
        return endsWithSlash.matcher(incDir).matches();
    }

    private List<String> listDirectory(File path, String rest) {
        List<String> fileNames = new ArrayList<String>();
        if(path != null && path.isDirectory()) {
            for(File file : path.listFiles(fileFilter)) {
                if(rest == null || rest.length() == 0)
                    fileNames.add(Parser.switchSpacesToEscapedSpacesInWord(file.getName()));
                else {
                    if(file.getName().startsWith(rest))
                        fileNames.add(Parser.switchSpacesToEscapedSpacesInWord(file.getName()));
                }
            }
        }

        return fileNames;
    }

    @Override
    public String toString() {
        return "FileLister{" +
                "incDir='" + incDir + '\'' +
                ", cwd=" + cwd +
                ", rest='" + rest + '\'' +
                ", lastDir='" + lastDir + '\'' +
                '}';
    }

    class DirectoryFileFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    }

    class FileAndDirectoryFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory() || pathname.isFile();
        }
    }

    //love this name :)
    class OnlyFileFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return pathname.isFile();
        }
    }

    public enum Filter { FILE,DIRECTORY,ALL}

}
