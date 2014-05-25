/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import org.jboss.aesh.console.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Resolve a file that might contain (~,*,?) to its proper parentPath
 * Returns a list of files.
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class PathResolver {

    private static final char SEPARATOR = Config.getPathSeparator().charAt(0);
    private static final char TILDE = '~';
    private static final String TILDE_WITH_SEPARATOR = "~"+Config.getPathSeparator();
    private static final char STAR = '*';
    private static final char WILDCARD = '?';
    private static final String PARENT = "..";
    private static final String PARENT_WITH_SEPARATOR = ".."+ Config.getPathSeparator();
    private static final String ROOT = Config.getPathSeparator();
    private static final String CURRENT_WITH_SEPARATOR = "."+Config.getPathSeparator();
    private static final String SEPARATOR_WITH_CURRENT = Config.getPathSeparator()+".";
    private static final String SEPARATOR_CURRENT_SEPARATOR = Config.getPathSeparator()+"."+Config.getPathSeparator();
    private static final String CURRENT = ".";
    private static Pattern starPattern = Pattern.compile("[\\*]+");

    /**
     * 1. find the absolute root directory
     * 2. check for wildcards
     *
     * @param incPath
     * @param cwd
     * @return
     */
    @SuppressWarnings("IndexOfReplaceableByContains")
    public static List<File> resolvePath(File incPath, File cwd) {
        if(cwd == null)
            cwd = new File(Config.getHomeDir());

        //if incPath start with eg: ./, remove it
        if(incPath.toString().startsWith(CURRENT_WITH_SEPARATOR)) {
            incPath = new File(incPath.toString().substring(CURRENT_WITH_SEPARATOR.length()));
        }

        //if incPath == ., clear it
        if(incPath.toString().equals(CURRENT))
            incPath = new File("");

        if(incPath.toString().startsWith(TILDE_WITH_SEPARATOR)) {
            if(Config.getHomeDir().endsWith(Config.getPathSeparator()))
                incPath = new File(Config.getHomeDir()+incPath.toString().substring(2));
            else
                incPath = new File(Config.getHomeDir()+incPath.toString().substring(1));
        }

        if(incPath.toString().indexOf(TILDE) == 0) {
            if(incPath.toString().length() > 1) {
                // directories which name starts with tilde
                incPath = new File(cwd.toString() + Config.getPathSeparator() + incPath.toString());
            } else {
                incPath = new File(Config.getHomeDir());
            }
        }

        //  foo1/./foo2 is changed to foo1/foo2
        if(incPath.toString().indexOf(SEPARATOR_CURRENT_SEPARATOR) > -1) {
            int index = incPath.toString().indexOf(SEPARATOR_CURRENT_SEPARATOR);
            if(index == 0) {
                incPath = new File(incPath.toString().substring(SEPARATOR_CURRENT_SEPARATOR.length()-1));
            }
            else {
                incPath = new File(incPath.toString().substring(0, index) +
                        incPath.toString().substring(index+2, incPath.toString().length()));
            }
        }

        // foo1/foo2/. is changed to foo1/foo2
        if(incPath.toString().endsWith(SEPARATOR_WITH_CURRENT)) {
            incPath = new File(incPath.toString().substring(0, incPath.toString().length()-SEPARATOR_WITH_CURRENT.length()));
        }

        //parentPath do not start with / and cwd is not / either
        if(incPath.toString().indexOf(ROOT) != 0 && !cwd.toString().equals(ROOT)) {
            if(cwd.toString().endsWith(Config.getPathSeparator()))
                incPath = new File(cwd.toString() + incPath.toString());
            else
                incPath = new File(cwd.toString() + Config.getPathSeparator() + incPath.toString());
        }

        if(incPath.toString().indexOf(PARENT_WITH_SEPARATOR) > -1) {
            String tmp = incPath.toString();
            while(tmp.indexOf(PARENT_WITH_SEPARATOR) > -1) {
                int index = tmp.indexOf(PARENT_WITH_SEPARATOR);
                if(index == 0) {
                    tmp = tmp.substring(PARENT_WITH_SEPARATOR.length());
                }
                else {
                    File tmpFile = new File(tmp.substring(0, index));
                    tmpFile = tmpFile.getParentFile();
                    if(tmpFile == null)
                        tmpFile = new File(Config.getPathSeparator());
                    tmpFile = new File(tmpFile.toString() + tmp.substring(index+ PARENT_WITH_SEPARATOR.length()-1));
                    //tmp = tmp.substring(0, index) + tmp.substring(index+PARENT_WITH_SEPARATOR.length());
                    tmp = tmpFile.toString();
                }
            }
            incPath = new File(tmp);
        }

        if(incPath.toString().endsWith(PARENT)) {
            incPath = new File(incPath.toString().substring(0, incPath.toString().length()-PARENT.length()));
            incPath = incPath.getParentFile();
            if(incPath == null)
                incPath = new File(Config.getPathSeparator());
        }

        if( incPath.toString().indexOf(STAR) > -1 || incPath.toString().indexOf(WILDCARD) > -1) {
            PathCriteria pathCriteria = parsePath(incPath);
            if(incPath.toString().indexOf(SEPARATOR) > -1) {
                List<File> foundFiles  = null;
                if(pathCriteria.getCriteria().equals(String.valueOf(STAR))) {
                    foundFiles = new ArrayList<>();
                    foundFiles.add(new File(pathCriteria.getParentPath()));
                }
                else
                    foundFiles = findFiles(new File(pathCriteria.parentPath), pathCriteria.getCriteria(), false);
                if(pathCriteria.childPath.length() == 0)
                    return foundFiles;
                else {
                    List<File> outFiles = new ArrayList<>();
                    for(File f : foundFiles)
                        if(new File(f+Config.getPathSeparator()+pathCriteria.childPath).exists())
                            outFiles.add(new File(f+Config.getPathSeparator()+pathCriteria.childPath));

                    return outFiles;
                }
            }
            //just wildcard without separators
            else {
                if(incPath.toString().length() == 1) {
                    List<File> foundFiles = findFiles(new File(pathCriteria.parentPath), pathCriteria.getCriteria(), false);
                    if(pathCriteria.childPath.length() == 0)
                        return foundFiles;
                }

                return new ArrayList<File>();
            }
        }
        else {
            //no wildcards
            ArrayList<File> fileList = new ArrayList<>(1);
            fileList.add(incPath);
            return fileList;
        }
    }

    private static List<File> parseWildcard(File incPath) {
        ArrayList<File> files = new ArrayList<>();
        int index = -1;
        while(incPath.toString().indexOf(STAR) > -1) {

        }

        return files;
    }

    private static List<File> findFiles(File incPath, String searchArgument, boolean findDirectory) {
        ArrayList<File> files = new ArrayList<>();

        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**");
         DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry)  {
                return matcher.matches(entry.getFileName());
            }
        };

        if(starPattern.matcher(searchArgument).matches()) {
            try (DirectoryStream<Path> stream = findDirectory ?
                         Files.newDirectoryStream(incPath.toPath(), new DirectoryFilter()) :
                    Files.newDirectoryStream(incPath.toPath(), new FileFilter())) {
                for(Path p : stream)
                    files.add(p.toFile());
                return files;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(incPath.toPath(), searchArgument)) {
                if(findDirectory) {
                    for(Path p : stream)
                        if(Files.isDirectory(p))
                            files.add(p.toFile());
                }
                else {
                    for(Path p : stream)
                        files.add(p.toFile());
                }
                return files;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return files;
    }

    private static class DirectoryFilter implements DirectoryStream.Filter<Path> {
        @Override
        public boolean accept(Path entry) throws IOException {
            return Files.isDirectory(entry);
        }
    }

    private static class FileFilter implements DirectoryStream.Filter<Path> {
        @Override
        public boolean accept(Path entry) throws IOException {
            return Files.exists(entry);
        }
    }

    //todo: path criteria need to check if separator is in the path
    private static PathCriteria parsePath(File path) {
        int starIndex = path.toString().indexOf(STAR);
        int wildcardIndex = path.toString().indexOf(WILDCARD);

        int index = (starIndex < wildcardIndex || wildcardIndex < 0) ? starIndex : wildcardIndex;

        if(index == 0 && path.toString().length() == 1)
            return new PathCriteria(String.valueOf(SEPARATOR), "", path.toString());
        else {
            int parentSeparatorIndex = index-1;
            while(path.toString().charAt(parentSeparatorIndex) != SEPARATOR && parentSeparatorIndex > -1)
                parentSeparatorIndex--;

            int childSeparatorIndex = index+1;
            if(childSeparatorIndex < path.toString().length())
                while(path.toString().charAt(childSeparatorIndex) != SEPARATOR && parentSeparatorIndex < path.toString().length())
                    childSeparatorIndex++;

            String parentPath = path.toString().substring(0, parentSeparatorIndex);
            String criteria = path.toString().substring(parentSeparatorIndex+1, childSeparatorIndex);
            String childPath = path.toString().substring(childSeparatorIndex, path.toString().length());

            return new PathCriteria(parentPath, childPath, criteria);
        }
    }

    static class PathCriteria {
        private String parentPath;
        private String childPath;
        private String criteria;

        PathCriteria(String parentPath, String childPath, String criteria) {
            this.parentPath = parentPath;
            this.childPath = childPath;
            this.criteria = criteria;
        }

        public String getParentPath() {
            return parentPath;
        }

        public String getCriteria() {
            return criteria;
        }

        public String getChildPath() {
            return childPath;
        }
    }
}
