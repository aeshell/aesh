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
            createLogHandlerToFile(logFile);

            if(logFile.isDirectory()) {
                logFile = new File(logFile.getAbsolutePath()+ Config.getPathSeparator()+"aesh.log");
            }
            createLogHandler(new FileHandler(logFile.getAbsolutePath()));
        }
        catch (IOException e) {
            createLogHandler(new ConsoleHandler());
        }

    }

    private static void createLogHandlerToFile(File logFile) {
        if(isCreateANewHandler(logFile)) {
            createLogHandler(new ConsoleHandler());
            return;
        }
    }

    private static boolean isCreateANewHandler(File logFile) {
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
