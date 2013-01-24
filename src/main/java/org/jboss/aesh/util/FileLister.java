/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
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

    private static Logger logger = LoggerUtil.getLogger(FileLister.class.getName());

    public FileLister(String incDir, File cwd) {
        if(incDir == null)
            throw new IllegalArgumentException("Incoming directory cannot be null");
        if(cwd == null)
            throw new IllegalArgumentException("Current working directory cannot be null");
        this.incDir = Parser.switchEscapedSpacesToSpacesInWord(incDir);
        this.cwd = cwd;
        findRestAndLastDir();
        logger.info("FileLister: "+this.toString());
    }

    public void findMatchingDirectories(CompleteOperation completion) {
       completion.doAppendSeparator(false);


        //if incDir is empty, just list cwd
        if(incDir.trim().isEmpty()) {
            completion.addCompletionCandidates( listDirectory(cwd, null));
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
        else if(startWithHome()) {

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
        else if(lastDir != null)
            returnFiles =  listDirectory(new File(cwd+
                    Config.getPathSeparator()+lastDir), rest);
        else
            returnFiles =  listDirectory(cwd, rest);

        //try to find if more than one filename start with the same word
        if(returnFiles.size() > 1) {
            String startsWith = Parser.findStartsWith(returnFiles);
            if(startsWith != null && startsWith.length() > 0 &&
                    startsWith.length() > rest.length()) {
                completion.addCompletionCandidate(Parser.switchSpacesToEscapedSpacesInWord( startsWith.substring(rest.length())));
            }
            //need to list complete filenames
            else {
                completion.addCompletionCandidates(returnFiles);
            }
        }
        else if(returnFiles.size() == 1)
            completion.addCompletionCandidate(returnFiles.get(0).substring(rest.length()));

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

    private boolean startWithParent() {
        return startsWithParent.matcher(incDir).matches();
    }

    private boolean startWithHome() {
        return startsWithHomePattern.matcher(incDir).matches();
    }

    private boolean startWithSlash() {
        return startsWithSlash.matcher(incDir).matches();
    }

    private boolean endWithSlash() {
        return endsWithSlash.matcher(incDir).matches();
    }

    private List<String> listDirectory(File path, String rest) {
        List<String> fileNames = new ArrayList<String>();
        if(path != null && path.isDirectory())
            for(File file : path.listFiles()) {
                if(rest == null || rest.length() == 0)
                    //fileNames.add(file.getName());
                    fileNames.add(Parser.switchSpacesToEscapedSpacesInWord(file.getName()));
                else {
                    if(file.getName().startsWith(rest))
                        //fileNames.add(file.getName());
                        fileNames.add(Parser.switchSpacesToEscapedSpacesInWord(file.getName()));
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
}
