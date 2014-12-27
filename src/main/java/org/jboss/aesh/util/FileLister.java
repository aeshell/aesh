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
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @author <a href="mailto:danielsoro@gmail.com>Daniel Cunha (soro)</a>
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
        if (checkEmptyToken(completion)) return;
        if (checkNotIsWindowsDriver(completion)) return;
        checkContainsStarAndWildCards(completion);
        checkStartWithHomeOrIsTokenADirectoryOrAFileOrEndsWithSlash(completion);
        checkStartWithSlashOwWithWindowsDriverOrTokeADiractory(completion);
        checkTokeAFile(completion);
    }

    private void checkTokeAFile(CompleteOperation completion) {
        listPossibleDirectories(completion);
        if (isTokenAFile()) {
            if (completion.getCompletionCandidates().size() == 1) {
                completion.getCompletionCandidates().set(0, new TerminalString("", true));
                completion.doAppendSeparator(true);
            }
        }
        findIfMoreThanOneFileStartWithSameWord(completion);
        newOffsetTweakingToMatchTheCommon(completion);
    }

    private void checkStartWithSlashOwWithWindowsDriverOrTokeADiractory(CompleteOperation completion) {
        if ((startWithSlash() || startWithWindowsDrive()) && (isTokenADirectory() && tokenEndsWithSlash())) {
            completion.addCompletionCandidates(listDirectory(cwd.newInstance(token), null));
        } else {
            completion.addCompletionCandidate(Config.getPathSeparator());
        }
        findIfMoreThanOneFileStartWithSameWord(completion);
        newOffsetTweakingToMatchTheCommon(completion);
    }

    private boolean checkNotIsWindowsDriver(CompleteOperation completion) {
        if (startWithSlash() && startWithWindowsDrive()) {
            return false;
        }

        if (isCwdAndTokenADirectory()) {
            if (tokenEndsWithSlash()) {
                completion.addCompletionCandidates(
                        listDirectory(cwd.newInstance(cwd.getAbsolutePath() +
                                Config.getPathSeparator() + token), null));
            } else {
                List<String> tmpDirs;
                if (lastDir != null) {
                    tmpDirs = listDirectory(
                            cwd.newInstance(cwd.getAbsolutePath() + Config.getPathSeparator() + lastDir), rest);
                } else {
                    tmpDirs = listDirectory(cwd, rest);
                }
                if (tmpDirs.size() == 1 || endsWithParent())
                    completion.addCompletionCandidate(rest + Config.getPathSeparator());
                else
                    completion.addCompletionCandidates(tmpDirs);
            }
            findIfMoreThanOneFileStartWithSameWord(completion);
            newOffsetTweakingToMatchTheCommon(completion);
            return true;
        }

        if (isCwdAndTokenAFile()) {
            listPossibleDirectories(completion);
            if (completion.getCompletionCandidates().size() == 1) {
                completion.getCompletionCandidates().set(0, new TerminalString("", true));
                completion.doAppendSeparator(true);
            } else if (completion.getCompletionCandidates().size() == 0) {
                completion.addCompletionCandidate("");
                completion.doAppendSeparator(true);
            }
        } else {
            listPossibleDirectories(completion);
        }
        findIfMoreThanOneFileStartWithSameWord(completion);
        newOffsetTweakingToMatchTheCommon(completion);
        return true;

    }

    private void checkStartWithHomeOrIsTokenADirectoryOrAFileOrEndsWithSlash(CompleteOperation completion) {
        if (!startWithHome() || !isHomeAndTokenADirectory()) {
            return;
        }

        if (tokenEndsWithSlash()) {
            completion.addCompletionCandidates(listDirectory(cwd.newInstance(Config.getHomeDir() + token.substring(1)), null));
            findIfMoreThanOneFileStartWithSameWord(completion);
            newOffsetTweakingToMatchTheCommon(completion);
            return;
        }

        if (isHomeAndTokenAFile()) {
            completion.addCompletionCandidate("");
            completion.doAppendSeparator(true);
            return;
        }

        List<String> tmpDirs = listDirectory(cwd.newInstance(Config.getHomeDir()), token.substring(2));
        if (tmpDirs.size() == 1 || endsWithParent()) {
            completion.addCompletionCandidate(Config.getPathSeparator());
        } else {
            completion.addCompletionCandidates(tmpDirs);
        }

        findIfMoreThanOneFileStartWithSameWord(completion);
        newOffsetTweakingToMatchTheCommon(completion);
        listPossibleDirectories(completion);
    }

    private void checkContainsStarAndWildCards(CompleteOperation completion) {
        if (containStar() || containWildCards()) {
            findIfMoreThanOneFileStartWithSameWord(completion);
            newOffsetTweakingToMatchTheCommon(completion);
        }
    }

    private boolean checkEmptyToken(CompleteOperation completion) {
        if (isTokenEmpty()) {
            completion.addCompletionCandidates(listDirectory(cwd, null));
            findIfMoreThanOneFileStartWithSameWord(completion);
            newOffsetTweakingToMatchTheCommon(completion);
            return true;
        }
        return false;
    }

    private void newOffsetTweakingToMatchTheCommon(CompleteOperation completion) {
        if (completion.getCompletionCandidates().size() == 1) {
            TerminalString terminalString = completion.getCompletionCandidates().get(0);
            if (isTokenADirectory() && !tokenEndsWithSlash() && (startWithSlash() || startWithWindowsDrive())) {
                terminalString.setCharacters(token + terminalString.getCharacters());
                completion.setOffset(completion.getCursor() - token.length());
                return;
            }

            if (token == null) {
                return;
            }

            if (rest != null && token.length() > rest.length()) {
                terminalString.setCharacters(Parser.switchSpacesToEscapedSpacesInWord(token.substring(0, token.length() - rest.length())) + terminalString.getCharacters());
                completion.setOffset(completion.getCursor() - token.length());
            }

            if (rest != null && token.length() == rest.length()) {
                completion.setOffset(completion.getCursor() - (rest.length() + Parser.findNumberOfSpacesInWord(rest)));
            }

            if (token.endsWith(Config.getPathSeparator())) {
                terminalString.setCharacters(Parser.switchSpacesToEscapedSpacesInWord(token) + terminalString.getCharacters());
            }
            completion.setOffset(completion.getCursor() - token.length());
        } else {
            completion.setOffset(completion.getCursor());
        }

        if (completion.getCompletionCandidates().size() > 1) {
            if (rest == null || rest.isEmpty()) {
                return;
            }
            completion.setIgnoreOffset(true);
            completion.setOffset(completion.getCursor() - rest.length());
        }

    }

    private void findIfMoreThanOneFileStartWithSameWord(CompleteOperation completion) {
        if (completion.getCompletionCandidates().size() > 1) {
            String startsWith = Parser.findStartsWithTerminalString(completion.getCompletionCandidates());
            startsWith = Parser.switchEscapedSpacesToSpacesInWord(startsWith);

            if (startsWith != null && startsWith.length() > 0 && rest != null && startsWith.length() > rest.length()) {
                completion.getCompletionCandidates().clear();
                completion.addCompletionCandidate(Parser.switchSpacesToEscapedSpacesInWord(startsWith));
            }
        }
    }

    private boolean isTokenEmpty() {
        return token.trim().isEmpty();
    }

    private void listPossibleDirectories(CompleteOperation completion) {
        List<String> returnFiles;

        if (startWithSlash() && lastDir != null && lastDir.startsWith(Config.getPathSeparator())) {
            returnFiles = listDirectory(cwd.newInstance(lastDir), rest);
        } else {
            returnFiles = listDirectory(cwd.newInstance(Config.getPathSeparator() + lastDir), rest);
        }

        if (startWithWindowsDrive()) {
            if (lastDir != null && lastDir.length() == 2)
                returnFiles = listDirectory(cwd.newInstance(lastDir + Config.getPathSeparator()), rest);
            else
                returnFiles = listDirectory(cwd.newInstance(lastDir), rest);
        }

        if (startWithHome() && lastDir != null) {
            returnFiles = listDirectory(cwd.newInstance(Config.getHomeDir() + lastDir.substring(1)), rest);
        } else {
            returnFiles = listDirectory(cwd.newInstance(Config.getHomeDir() + Config.getPathSeparator()), rest);
        }

        if (lastDir != null) {
            returnFiles = listDirectory(cwd.newInstance(cwd + Config.getPathSeparator() + lastDir), rest);
        } else
            returnFiles = listDirectory(cwd, rest);

        completion.addCompletionCandidates(returnFiles);
    }

    private void findRestAndLastDir() {
        if (token.contains(Config.getPathSeparator())) {
            lastDir = token.substring(0, token.lastIndexOf(Config.getPathSeparator()));
            rest = token.substring(token.lastIndexOf(Config.getPathSeparator()) + 1);
            return;
        }

        if (isTokenEmpty()) {
            lastDir = token;
            return;
        }
        rest = token;
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
            return false;
        }

        for (Resource f : cwd.listRoots()) {
            if (token.startsWith(f.toString())) {
                return true;
            }
        }
        return false;
    }

    private boolean tokenEndsWithSlash() {
        return token.lastIndexOf(Config.getPathSeparator()) == token.length() - 1;
    }

    private List<String> listDirectory(Resource path, String rest) {
        List<String> fileNames = new ArrayList<String>();
        if (path == null && path.isLeaf()) {
            instanceFileComparatorAndSortCollection(fileNames);
            return fileNames;
        }

        for (Resource file : path.list(fileFilter)) {
            if (rest == null || rest.length() == 0) {
                if (!file.isLeaf())
                    fileNames.add(Parser.switchSpacesToEscapedSpacesInWord(file.getName()) + Config.getPathSeparator());
                else
                    fileNames.add(Parser.switchSpacesToEscapedSpacesInWord(file.getName()));
                continue;
            }

            if (file.getName().startsWith(rest)) {
                if (!file.isLeaf())
                    fileNames.add(Parser.switchSpacesToEscapedSpacesInWord(file.getName()) + Config.getPathSeparator());
                else
                    fileNames.add(Parser.switchSpacesToEscapedSpacesInWord(file.getName()));
            }
        }

        instanceFileComparatorAndSortCollection(fileNames);
        return fileNames;
    }

    private void instanceFileComparatorAndSortCollection(List<String> fileNames) {
        if (fileComparator == null) {
            fileComparator = new PosixFileNameComparator();
        }
        Collections.sort(fileNames, fileComparator);
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
