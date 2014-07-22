/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import static org.jboss.aesh.constants.AeshConstants.ESCAPE;
import static org.jboss.aesh.constants.AeshConstants.HOME;
import static org.jboss.aesh.constants.AeshConstants.PARENT;
import static org.jboss.aesh.constants.AeshConstants.STAR;
import static org.jboss.aesh.constants.AeshConstants.WILDCARD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jboss.aesh.comparators.PosixFileNameComparator;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.io.filter.AllResourceFilter;
import org.jboss.aesh.io.Resource;
import org.jboss.aesh.io.filter.ResourceFilter;
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.terminal.TerminalString;

/**
 * Helper class to list possible files during a complete operation.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class FileLister {

    private String token;
    private Resource cwd;
    private String rest;
    private String lastDir;
    private ResourceFilter fileFilter;
    private Comparator fileComparator;

    public FileLister(String token, Resource cwd) {
        if (token == null)
            throw new IllegalArgumentException("Incoming directory cannot be null");
        if (cwd == null)
            throw new IllegalArgumentException("Current working directory cannot be null");
        this.token = Parser.switchEscapedSpacesToSpacesInWord(token);
        this.cwd = cwd;
        findRestAndLastDir();
        setFileFilter(new AllResourceFilter());
    }

    public FileLister(String token, Resource cwd, Comparator comparator) {
        this(token, cwd);
        this.fileComparator = comparator;
    }

    public FileLister(String token, Resource cwd, ResourceFilter filter) {
        this(token, cwd);
        setFileFilter(filter);
    }

    public FileLister(String token, Resource cwd, ResourceFilter filter, Comparator fileComparator) {
        this(token, cwd, filter);
        this.fileComparator = fileComparator;
    }

    private void setFileFilter(ResourceFilter filter) {
        this.fileFilter = filter;
    }

    /**
     * findMatchingDirectories will try to populate the CompleteOperation object based on it initial params.
     *
     * @param completion
     */
    public void findMatchingDirectories(CompleteOperation completion) {
        completion.doAppendSeparator(false);

        // if token is empty, just list cwd
        if (token.trim().isEmpty()) {
            completion.addCompletionCandidates(listDirectory(cwd, null));
        }
        else if (containStar() || containWildCards()) {
        }
        else if (startWithHome()) {
            if (isHomeAndTokenADirectory()) {
                if (tokenEndsWithSlash()) {
                    completion.addCompletionCandidates(
                        listDirectory(cwd.newInstance(Config.getHomeDir() + token.substring(1)), null));
                }
                else {
                    // completion.addCompletionCandidate(Config.getPathSeparator());
                    List<String> tmpDirs = listDirectory(cwd.newInstance(Config.getHomeDir()), token.substring(2));
                    if (tmpDirs.size() == 1 || endsWithParent()) {
                        completion.addCompletionCandidate(Config.getPathSeparator());
                    }
                    else
                        completion.addCompletionCandidates(tmpDirs);
                }
            }
            else if (isHomeAndTokenAFile()) {
                completion.addCompletionCandidate("");
                // append when we have a file
                completion.doAppendSeparator(true);
            }
            // not a directory or file, list what we find
            else {
                listPossibleDirectories(completion);
            }
        }
        else if (!startWithSlash() && !startWithWindowsDrive()) {
            if (isCwdAndTokenADirectory()) {
                if (tokenEndsWithSlash()) {
                    completion.addCompletionCandidates(
                        listDirectory(cwd.newInstance(cwd.getAbsolutePath() +
                            Config.getPathSeparator() + token), null));
                }
                else {
                    List<String> tmpDirs;
                    if (lastDir != null) {
                        tmpDirs = listDirectory(
                            cwd.newInstance(cwd.getAbsolutePath() + Config.getPathSeparator() + lastDir), rest);
                    }
                    else {
                        tmpDirs = listDirectory(cwd, rest);
                    }
                    if (tmpDirs.size() == 1 || endsWithParent())
                        completion.addCompletionCandidate(rest + Config.getPathSeparator());
                    else
                        completion.addCompletionCandidates(tmpDirs);
                }
            }
            else if (isCwdAndTokenAFile()) {
                listPossibleDirectories(completion);
                if (completion.getCompletionCandidates().size() == 1) {
                    completion.getCompletionCandidates().set(0, new TerminalString("", true));
                    // append when we have a file
                    completion.doAppendSeparator(true);
                }
                else if (completion.getCompletionCandidates().size() == 0) {
                    completion.addCompletionCandidate("");
                    // append when we have a file
                    completion.doAppendSeparator(true);
                }
            }
            // not a directory or file, list what we find
            else {
                listPossibleDirectories(completion);
            }
        }
        else if (startWithSlash() || startWithWindowsDrive()) {
            if (isTokenADirectory()) {
                if (tokenEndsWithSlash()) {
                    completion.addCompletionCandidates(
                        listDirectory(cwd.newInstance(token), null));
                }
                else
                    completion.addCompletionCandidate(Config.getPathSeparator());
            }
            else if (isTokenAFile()) {
                listPossibleDirectories(completion);
                if (completion.getCompletionCandidates().size() == 1) {
                    completion.getCompletionCandidates().set(0, new TerminalString("", true));
                    // completion.addCompletionCandidate("");
                    // append when we have a file
                    completion.doAppendSeparator(true);
                }
            }
            // not a directory or file, list what we find
            else {
                listPossibleDirectories(completion);
            }
        }

        // try to find if more than one filename start with the same word
        if (completion.getCompletionCandidates().size() > 1) {
            String startsWith = Parser.findStartsWithTerminalString(completion.getCompletionCandidates());
            if (startsWith.contains(" "))
                startsWith = Parser.switchEscapedSpacesToSpacesInWord(startsWith);
            if (startsWith != null && startsWith.length() > 0 && rest != null &&
                startsWith.length() > rest.length()) {
                completion.getCompletionCandidates().clear();
                completion.addCompletionCandidate(Parser.switchSpacesToEscapedSpacesInWord(startsWith));
            }
        }

        // new offset tweaking to match the "common" way of returning completions
        if (completion.getCompletionCandidates().size() == 1) {
            if (isTokenADirectory() && !tokenEndsWithSlash() && (startWithSlash() || startWithWindowsDrive())) {
                completion.getCompletionCandidates().get(0).setCharacters(token +
                    completion.getCompletionCandidates().get(0).getCharacters());

                completion.setOffset(completion.getCursor() - token.length());
            }
            else if (token != null) {
                if (rest != null && token.length() > rest.length()) {
                    completion.getCompletionCandidates().get(0).setCharacters(
                        Parser.switchSpacesToEscapedSpacesInWord(
                            token.substring(0, token.length() - rest.length())) +
                            completion.getCompletionCandidates().get(0).getCharacters());

                    completion.setOffset(completion.getCursor() - token.length());
                }
                else if (rest != null && token.length() == rest.length()) {
                    completion.setOffset(completion.getCursor() - (rest.length() + Parser.findNumberOfSpacesInWord(rest)));
                }
                else {
                    if (token.endsWith(Config.getPathSeparator()))
                        completion.getCompletionCandidates().get(0).setCharacters(
                            Parser.switchSpacesToEscapedSpacesInWord(token) +
                                completion.getCompletionCandidates().get(0).getCharacters());

                    completion.setOffset(completion.getCursor() - token.length());
                }
            }
            else {
                completion.setOffset(completion.getCursor());
            }
        }
        else if (completion.getCompletionCandidates().size() > 1) {
            completion.setIgnoreOffset(true);
            if (rest != null && rest.length() > 0)
                completion.setOffset(completion.getCursor() - rest.length());
        }
    }

    private void listPossibleDirectories(CompleteOperation completion) {
        List<String> returnFiles;

        if (startWithSlash()) {
            if (lastDir != null && lastDir.startsWith(Config.getPathSeparator()))
                returnFiles = listDirectory(cwd.newInstance(lastDir), rest);
            else
                returnFiles = listDirectory(cwd.newInstance(Config.getPathSeparator() + lastDir), rest);
        }
        else if (startWithWindowsDrive()) {
            if (lastDir != null && lastDir.length() == 2)
                returnFiles = listDirectory(cwd.newInstance(lastDir + Config.getPathSeparator()), rest);
            else
                returnFiles = listDirectory(cwd.newInstance(lastDir), rest);
        }
        else if (startWithHome()) {
            if (lastDir != null) {
                returnFiles = listDirectory(cwd.newInstance(Config.getHomeDir() + lastDir.substring(1)), rest);
            }
            else
                returnFiles = listDirectory(cwd.newInstance(Config.getHomeDir() + Config.getPathSeparator()), rest);
        }
        else if (lastDir != null) {
            returnFiles = listDirectory(cwd.newInstance(cwd + Config.getPathSeparator() + lastDir), rest);
        }
        else
            returnFiles = listDirectory(cwd, rest);

        completion.addCompletionCandidates(returnFiles);
    }

    private void findRestAndLastDir() {
        if (token.contains(Config.getPathSeparator())) {
            lastDir = token.substring(0, token.lastIndexOf(Config.getPathSeparator()));
            rest = token.substring(token.lastIndexOf(Config.getPathSeparator()) + 1);
        }
        else if (token.trim().isEmpty()) {
            lastDir = token;
        }
        else {
            rest = token;
        }
    }

    private boolean isTokenADirectory() {
        return cwd.newInstance(token).isDirectory();
    }

    private boolean isTokenAFile() {
        return cwd.newInstance(token).isLeaf();
    }

    private boolean isCwdAndTokenADirectory() {
        return cwd.newInstance(cwd.getAbsolutePath() +
            Config.getPathSeparator() + token).isDirectory();
    }

    private boolean isCwdAndTokenAFile() {
        return cwd.newInstance(cwd.getAbsolutePath() +
                Config.getPathSeparator() + token).isLeaf();
    }

    private boolean isHomeAndTokenADirectory() {
        return cwd.newInstance(Config.getHomeDir() + token.substring(1)).isDirectory();
    }

    private boolean isHomeAndTokenAFile() {
        return cwd.newInstance(Config.getHomeDir() + token.substring(1)).isLeaf();
    }

    private boolean endsWithParent() {
        return token.length() > 1 && token.lastIndexOf(PARENT) == token.length() - 2;
    }

    private boolean startWithHome() {
        return token.indexOf(HOME) == 0;
    }

    private boolean containStar() {
        int index = token.indexOf(STAR);
        return index == 0 || (index > 0 && !(token.charAt(index - 1) == ESCAPE));
    }

    private boolean containWildCards() {
        int index = token.indexOf(WILDCARD);
        return index == 0 || (index > 0 && !(token.charAt(index - 1) == ESCAPE));
    }

    private boolean startWithSlash() {
        return token.indexOf(Config.getPathSeparator()) == 0;
    }

    private boolean startWithWindowsDrive() {
        if (!Config.isOSPOSIXCompatible()) {
            for (Resource f : cwd.listRoots()) {
                if (token.startsWith(f.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tokenEndsWithSlash() {
        return token.lastIndexOf(Config.getPathSeparator()) == token.length() - 1;
    }

    private List<String> listDirectory(Resource path, String rest) {
        List<String> fileNames = new ArrayList<String>();
        if (path != null && !path.isLeaf()) {
            for (Resource file : path.list(fileFilter)) {
                if (rest == null || rest.length() == 0)
                    if (!file.isLeaf())
                        fileNames.add(Parser.switchSpacesToEscapedSpacesInWord(file.getName()) + Config.getPathSeparator());
                    else
                        fileNames.add(Parser.switchSpacesToEscapedSpacesInWord(file.getName()));
                else {
                    if (file.getName().startsWith(rest)) {
                        if (!file.isLeaf())
                            fileNames.add(Parser.switchSpacesToEscapedSpacesInWord(file.getName()) + Config.getPathSeparator());
                        else
                            fileNames.add(Parser.switchSpacesToEscapedSpacesInWord(file.getName()));
                    }
                }
            }
        }

        if (fileComparator == null)
            fileComparator = new PosixFileNameComparator();
        Collections.sort(fileNames, fileComparator);
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
}
