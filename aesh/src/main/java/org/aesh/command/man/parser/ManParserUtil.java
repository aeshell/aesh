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
package org.aesh.command.man.parser;

import org.aesh.terminal.utils.ANSI;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ManParserUtil {

    private static final Pattern boldRegex = Pattern.compile("(\\*[^']+\\*)|(\'\\S+\')|(::$)");

    public static String convertStringToAnsi(String line) {
        StringBuilder builder = new StringBuilder();
        Matcher matcher = boldRegex.matcher(line);
        while(matcher.find()) {
            if(matcher.group(1) != null) {
                builder.append(line.substring(0,matcher.start(1)))
                        .append(ANSI.BOLD)
                        .append(line.substring(matcher.start(1)+1,matcher.end(1)-1))
                        .append(ANSI.DEFAULT_TEXT);
                        //.append(line.substring(matcher.end(1)));
                line = line.substring(matcher.end(1));
                matcher = boldRegex.matcher(line);
            }
            else if(matcher.group(2) != null) {
                builder.append(line.substring(0,matcher.start(2)))
                        .append(ANSI.UNDERLINE)
                        .append(line.substring(matcher.start(2)+1,matcher.end(2)-1))
                        .append(ANSI.DEFAULT_TEXT);
                        //.append(line.substring(matcher.end(2)));
                line = line.substring(matcher.end(2));
                matcher = boldRegex.matcher(line);
            }
            else if(matcher.group(3) != null) {
                builder.append(line.substring(0,matcher.start(3)));
                line = line.substring(matcher.end(3));
                matcher = boldRegex.matcher(line);
            }
        }
        if(line.length() > 0)
            builder.append(line);

        if(builder.length() < 1)
            return line;
        else
            return builder.toString();
    }
}
