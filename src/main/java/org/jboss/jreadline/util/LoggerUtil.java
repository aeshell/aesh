/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.jreadline.util;

import org.jboss.jreadline.console.settings.Settings;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * butt ugly logger util, but its simple and gets the job done (hopefully not too dangerous)
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class LoggerUtil {

    private static Handler logHandler;

    /**
     *
     * @param name class
     * @return logger
     */
    public static synchronized Logger getLogger(String name) {
        if(logHandler == null)
            try {
                logHandler = new FileHandler(Settings.getInstance().getLogFile());
                logHandler.setFormatter(new SimpleFormatter());
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        Logger log =  Logger.getLogger(name);
        log.setUseParentHandlers(false);
        log.addHandler(logHandler);

        return log;
    }
}
