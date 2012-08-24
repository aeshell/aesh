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
package org.jboss.jreadline.console.redirection;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class RedirectionParser {

    private static Pattern simpleRedirectPattern = Pattern.compile("<|>|\\|");
    private static Pattern overwriteOutAndErrPattern = Pattern.compile("2>&1");

    public static RedirectionOperation findNextRedirection(String buffer) {
        String command = buffer.trim();
        if(command.startsWith(Redirection.OVERWRITE_OUT_AND_ERR.getControlOperator()))
            return new RedirectionOperation(Redirection.OVERWRITE_OUT_AND_ERR,
                    findTextBeforeNextRedirection(buffer.substring(Redirection.OVERWRITE_OUT_AND_ERR.getControlOperator().length())));
        else if(command.startsWith(Redirection.APPEND_ERR.getControlOperator()))
            return new RedirectionOperation(Redirection.APPEND_ERR,
                    findTextBeforeNextRedirection(buffer.substring(Redirection.APPEND_ERR.getControlOperator().length())));
        else if(command.startsWith(Redirection.OVERWRITE_ERR.getControlOperator()))
            return new RedirectionOperation(Redirection.OVERWRITE_ERR,
                    findTextBeforeNextRedirection(buffer.substring(Redirection.OVERWRITE_ERR.getControlOperator().length())));
        else if(command.startsWith(Redirection.APPEND_OUT.getControlOperator()))
            return new RedirectionOperation(Redirection.APPEND_OUT,
                    findTextBeforeNextRedirection(buffer.substring(Redirection.APPEND_OUT.getControlOperator().length())));
        else if(command.startsWith(Redirection.OVERWRITE_IN.getControlOperator()))
            return new RedirectionOperation(Redirection.OVERWRITE_IN,
                    findTextBeforeNextRedirection(buffer.substring(Redirection.OVERWRITE_IN.getControlOperator().length())));
        else if(command.startsWith(Redirection.OVERWRITE_OUT.getControlOperator()))
            return new RedirectionOperation(Redirection.OVERWRITE_OUT,
                    findTextBeforeNextRedirection(buffer.substring(Redirection.OVERWRITE_OUT.getControlOperator().length())));
        else if(command.startsWith(Redirection.PIPE_OUT_AND_ERR.getControlOperator()))
            return new RedirectionOperation(Redirection.PIPE_OUT_AND_ERR,
                    findTextBeforeNextRedirection(buffer.substring(Redirection.PIPE_OUT_AND_ERR.getControlOperator().length())));
        else if(command.startsWith(Redirection.PIPE.getControlOperator()))
            return new RedirectionOperation(Redirection.PIPE,
                    findTextBeforeNextRedirection(buffer.substring(Redirection.PIPE.getControlOperator().length())));
        else
            return new RedirectionOperation(Redirection.FIRST,
                    buffer.substring(Redirection.PIPE.getControlOperator().length()));
    }

    private static String findTextBeforeNextRedirection(String buffer) {
        int redirectPos = buffer.indexOf("<|>|\\|");
        if(redirectPos > 0)
            return buffer.substring(0, buffer.substring(0, redirectPos).indexOf(' '));
        else
            return buffer;
    }

    public static List<RedirectionOperation> parseBuffer(String buffer) {

        List<RedirectionOperation> redirectOps = new ArrayList<RedirectionOperation>();

        //first find the first pipe/redirect and put everything before that in the first op
        int bufferPos = -1;
        if((bufferPos = buffer.indexOf("<|>|\\|")) > -1) {
            int spacePos = buffer.substring(0, bufferPos).indexOf(" ");
            if(spacePos > -1) {
                redirectOps.add(new RedirectionOperation(Redirection.FIRST, buffer.substring(0, spacePos)));
                buffer = buffer.substring(spacePos);
            }
            else
                return redirectOps;
        }
        else {
            return redirectOps;
        }

        return redirectOps;
    }

    public static boolean matchesRedirectionOperation(String buffer) {
        return simpleRedirectPattern.matcher(buffer).find();
    }
}
