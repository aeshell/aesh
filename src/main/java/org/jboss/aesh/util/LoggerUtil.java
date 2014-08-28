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
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * butt ugly logger util, but its simple and gets the job done (hopefully not too dangerous)
 * warning: made it even uglier when Settings was changed to not be a Singleton... gah!
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 * @author <a href="mailto:danielsoro@gmail.com">Daniel Cunha (soro)</a>
 */
public class LoggerUtil {

    private static Handler logHandler;

    private static void createLogHandler(String log) {
        try {
            File logFile = new File(log);
            createDirsToParentFile(logFile);

            if(logFile.isDirectory()) {
                logFile = new File(logFile.getAbsolutePath()+ Config.getPathSeparator()+"aesh.log");
            }
            createLogHandler(new FileHandler(logFile.getAbsolutePath()));
        }
        catch (IOException e) {
            createLogHandler(new ConsoleHandler());
        }

    }

    private static void createDirsToParentFile(File logFile) {
        if(isCreateNewDirsToParent(logFile)) {
            createLogHandler(new ConsoleHandler());
            return;
        }
    }

    private static boolean isCreateNewDirsToParent(File logFile) {
        return logFile.getParentFile() != null && !logFile.getParentFile().isDirectory() && !logFile.getParentFile().mkdirs();
    }

    private static void createLogHandler(StreamHandler handler) {
        logHandler = handler;
        logHandler.setFormatter(new SimpleFormatter());
    }

    public static synchronized Logger getLogger(String name) {
        if(logHandler == null) {
            createLogHandler(Config.getTmpDir()+Config.getPathSeparator()+"aesh.log");
        }

        if(logHandler == null) {
            return Logger.getLogger(name);
        }

        Logger log =  Logger.getLogger(name);
        log.setUseParentHandlers(false);
        log.addHandler(logHandler);

        return log;
    }

}
