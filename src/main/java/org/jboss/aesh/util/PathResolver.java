/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.util;

import org.jboss.aesh.console.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolve a file that might contain (~,*,?) to its proper path
 * Returns a list of files.
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class PathResolver {

    private static final char TILDE = '~';
    private static final String TILDE_WITH_SEPARATOR = "~"+Config.getPathSeparator();
    private static final char STAR = '*';
    private static final char QUESTION = '?';
    private static final String PARENT = "..";
    private static final String PARENT_WITH_SEPARATOR = ".."+ Config.getPathSeparator();
    private static final String ROOT = Config.getPathSeparator();
    private static final String CURRENT_WITH_SEPARATOR = "."+Config.getPathSeparator();
    private static final String SEPARATOR_WITH_CURRENT = Config.getPathSeparator()+".";
    private static final String SEPARATOR_CURRENT_SEPARATOR = Config.getPathSeparator()+"."+Config.getPathSeparator();
    private static final String CURRENT = ".";

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
            if(incPath.toString().length() > 1)
                incPath = new File(Config.getPathSeparator() + incPath.toString().substring(1));
            else
                incPath = new File(Config.getPathSeparator());
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

        //path do not start with / and cwd is not / either
        if(incPath.toString().indexOf(ROOT) != 0 && !cwd.toString().equals(ROOT)) {
            if(cwd.toString().endsWith(Config.getPathSeparator()))
                incPath = new File(cwd.toString() + incPath.toString());
            else
                incPath = new File(cwd.toString() + Config.getPathSeparator() + incPath.toString());
        }

        if(incPath.toString().indexOf(PARENT_WITH_SEPARATOR) > -1) {
            String tmp = incPath.toString();
            File tmpFile = new File(incPath.toString());
            while(tmp.indexOf(PARENT_WITH_SEPARATOR) > -1) {
                int index = tmp.indexOf(PARENT_WITH_SEPARATOR);
                if(index == 0) {
                    tmp = tmp.substring(PARENT_WITH_SEPARATOR.length());
                    tmpFile = new File(Config.getPathSeparator());
                }
                else {
                    tmpFile = new File(tmp.substring(0, index));
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

        if( incPath.toString().indexOf(STAR) > -1) {
            int index = -1;

            //we need some wildcard parser
        }
        else {
            //no wildcards
            ArrayList<File> fileList = new ArrayList<File>(1);
            fileList.add(incPath);
            return fileList;
        }

        return null;
    }
    /*

    private static List<File> parseWildcard(File incPath) {
        ArrayList<File> files = new ArrayList<File>();
        int index = -1;
        while(incPath.toString().indexOf(STAR) > -1) {

        }

    }
    */

}
