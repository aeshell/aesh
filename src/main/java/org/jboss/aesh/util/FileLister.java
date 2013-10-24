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
import org.jboss.aesh.terminal.TerminalString;

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

    private String token;
    private File cwd;
    private String rest;
    private String lastDir;
    private FileFilter fileFilter;

    public FileLister(String token, File cwd) {
        if(token == null)
            throw new IllegalArgumentException("Incoming directory cannot be null");
        if(cwd == null)
            throw new IllegalArgumentException("Current working directory cannot be null");
        this.token = Parser.switchEscapedSpacesToSpacesInWord(token);
        this.cwd = cwd;
        findRestAndLastDir();
        setFileFilter(Filter.ALL);
    }

    public FileLister(String token, File cwd, Filter filter) {
        this(token, cwd);
        setFileFilter(filter);
    }

    public FileLister(String token, File cwd, FileFilter fileFilter) {
        this(token, cwd);
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

        //if token is empty, just list cwd
        if(token.trim().isEmpty()) {
            completion.addCompletionCandidates( listDirectory(cwd, null));
        }
        else if(startWithHome()) {
            if(isHomeAndTokenADirectory()) {
                if(tokenEndsWithSlash()) {
                    completion.addCompletionCandidates(
                            listDirectory(new File(Config.getHomeDir()+token.substring(1)), null));
                }
                else
                    completion.addCompletionCandidate(Config.getPathSeparator());
            }
            else if(isHomeAndTokenAFile()) {
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
            if(isCwdAndTokenADirectory()) {
                if(tokenEndsWithSlash()) {
                    completion.addCompletionCandidates(
                            listDirectory(new File(cwd.getAbsolutePath() +
                                    Config.getPathSeparator()+token), null));
                }
                else {
                   completion.addCompletionCandidates( listDirectory(cwd, token));
                }
            }
            else if(isCwdAndTokenAFile()) {
                listPossibleDirectories(completion);
                if(completion.getCompletionCandidates().size() == 1) {
                    completion.getCompletionCandidates().set(0, new TerminalString("", true));
                    //append when we have a file
                    completion.doAppendSeparator(true);
                }
                else if(completion.getCompletionCandidates().size() == 0) {
                    completion.addCompletionCandidate("");
                    //append when we have a file
                    completion.doAppendSeparator(true);
                }
            }
            //not a directory or file, list what we find
            else {
               listPossibleDirectories(completion);
            }
        }
        else if(startWithSlash()) {
            if(isTokenADirectory()) {
                if(tokenEndsWithSlash()) {
                    completion.addCompletionCandidates(
                            listDirectory(new File(token), null));
                }
                else
                    completion.addCompletionCandidate(Config.getPathSeparator());
            }
            else if(isTokenAFile()) {
                listPossibleDirectories(completion);
                if(completion.getCompletionCandidates().size() == 1) {
                    completion.getCompletionCandidates().set(0, new TerminalString("", true));
                    //completion.addCompletionCandidate("");
                    //append when we have a file
                    completion.doAppendSeparator(true);
                }
            }
            //not a directory or file, list what we find
            else {
               listPossibleDirectories(completion);
            }
        }

        //try to find if more than one filename start with the same word
        if(completion.getCompletionCandidates().size() > 1) {
            String startsWith = Parser.findStartsWithTerminalString(completion.getCompletionCandidates());
            if(startsWith.contains(" "))
                startsWith = Parser.switchEscapedSpacesToSpacesInWord(startsWith);
            if(startsWith != null && startsWith.length() > 0 && rest != null &&
                    startsWith.length() > rest.length()) {
                completion.getCompletionCandidates().clear();
                completion.addCompletionCandidate(Parser.switchSpacesToEscapedSpacesInWord(startsWith));
            }
        }

        //new offset tweaking to match the "common" way of returning completions
        if(completion.getCompletionCandidates().size() == 1) {
            if(isTokenADirectory() && !tokenEndsWithSlash()) {
                completion.getCompletionCandidates().get(0).setCharacters( token +
                        completion.getCompletionCandidates().get(0).getCharacters());

                completion.setOffset(completion.getCursor()-token.length());
            }
            else if(isTokenAFile()) {
                completion.getCompletionCandidates().get(0).setCharacters( token +
                        completion.getCompletionCandidates().get(0).getCharacters());

                completion.setOffset(completion.getCursor()-token.length());
                completion.doAppendSeparator(true);
            }
            else if(token != null) {
                if(rest != null && token.length() > rest.length()) {
                    completion.getCompletionCandidates().get(0).setCharacters(
                            Parser.switchSpacesToEscapedSpacesInWord(
                            token.substring(0, token.length()-rest.length() )) +
                                    completion.getCompletionCandidates().get(0).getCharacters());

                    completion.setOffset(completion.getCursor()-token.length());
                }
                else if(rest != null && token.length() == rest.length()) {
                    completion.setOffset(completion.getCursor()-(rest.length()+Parser.findNumberOfSpacesInWord(rest)));
                }
                else {
                    if(token.endsWith(Config.getPathSeparator()))
                        completion.getCompletionCandidates().get(0).setCharacters(
                                Parser.switchSpacesToEscapedSpacesInWord(token) +
                                completion.getCompletionCandidates().get(0).getCharacters());

                    completion.setOffset(completion.getCursor()-token.length());
                }
            }
            else {
                completion.setOffset(completion.getCursor());
            }
        }
        else if(completion.getCompletionCandidates().size() > 1) {
            if(rest != null && rest.length() > 0)
                completion.setOffset(completion.getCursor() - rest.length());
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


        completion.addCompletionCandidates(returnFiles);
    }

    private void findRestAndLastDir() {
        if(token.contains(Config.getPathSeparator())) {
            lastDir = token.substring(0, token.lastIndexOf(Config.getPathSeparator()));
            rest = token.substring(token.lastIndexOf(Config.getPathSeparator())+1);
        }
        else {
            if(new File(cwd+Config.getPathSeparator()+token).exists())
                lastDir = token;
            else {
                rest = token;
            }
        }
    }

    private boolean isTokenADirectory() {
        return new File(token).isDirectory();
    }

    private boolean isTokenAFile() {
        return new File(token).isFile();
    }

    private boolean isCwdAndTokenADirectory() {
        return new File(cwd.getAbsolutePath() +
                Config.getPathSeparator() + token).isDirectory();
    }

    private boolean isCwdAndTokenAFile() {
        return new File(cwd.getAbsolutePath() +
                Config.getPathSeparator() + token).isFile();
    }

    private boolean isHomeAndTokenADirectory() {
        return new File(Config.getHomeDir()+
                 token.substring(1)).isDirectory();
    }

    private boolean isHomeAndTokenAFile() {
        return new File(Config.getHomeDir()+
                token.substring(1)).isFile();
    }

    private boolean startWithParent() {
        return startsWithParent.matcher(token).matches();
    }

    private boolean startWithHome() {
        return token.startsWith("~/");
    }

    private boolean startWithSlash() {
        return startsWithSlash.matcher(token).matches();
    }

    private boolean tokenEndsWithSlash() {
        return endsWithSlash.matcher(token).matches();
    }

    private List<String> listDirectory(File path, String rest) {
        List<String> fileNames = new ArrayList<String>();
        if(path != null && path.isDirectory()) {
            for(File file : path.listFiles(fileFilter)) {
                if(rest == null || rest.length() == 0)
                    if(file.isDirectory())
                        fileNames.add(Parser.switchSpacesToEscapedSpacesInWord(file.getName())+Config.getPathSeparator());
                    else
                        fileNames.add(Parser.switchSpacesToEscapedSpacesInWord(file.getName()));
                else {
                    if(file.getName().startsWith(rest)) {
                        if(file.isDirectory())
                            fileNames.add(Parser.switchSpacesToEscapedSpacesInWord(file.getName())+Config.getPathSeparator());
                        else
                            fileNames.add(Parser.switchSpacesToEscapedSpacesInWord(file.getName()));
                    }
                }
            }
        }

        return fileNames;
    }

    @Override
    public String toString() {
        return "FileLister{" +
                "token='" + token + '\'' +
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
