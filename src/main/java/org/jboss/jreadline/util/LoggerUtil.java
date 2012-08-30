/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
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
    public synchronized static Logger getLogger(String name) {
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
