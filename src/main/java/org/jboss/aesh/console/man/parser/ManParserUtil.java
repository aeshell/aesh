/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.man.parser;

import org.jboss.aesh.util.ANSI;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ManParserUtil {

    private static Pattern boldRegex = Pattern.compile("(\\*[^']+\\*)|(\'\\S+\')|(::$)");

    public static String convertStringToAnsi(String line) {
        StringBuilder builder = new StringBuilder();
        Matcher matcher = boldRegex.matcher(line);
        while(matcher.find()) {
            if(matcher.group(1) != null) {
                builder.append(line.substring(0,matcher.start(1)))
                        .append(ANSI.getBold())
                        .append(line.substring(matcher.start(1)+1,matcher.end(1)-1))
                        .append(ANSI.defaultText());
                        //.append(line.substring(matcher.end(1)));
                line = line.substring(matcher.end(1));
                matcher = boldRegex.matcher(line);
            }
            else if(matcher.group(2) != null) {
                builder.append(line.substring(0,matcher.start(2)))
                        .append(ANSI.getUnderline())
                        .append(line.substring(matcher.start(2)+1,matcher.end(2)-1))
                        .append(ANSI.defaultText());
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
