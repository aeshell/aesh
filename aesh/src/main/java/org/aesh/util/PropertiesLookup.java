package org.aesh.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertiesLookup {
    public static final Pattern systemProperties = Pattern.compile("^\\$\\{((env:)|(sys:))?((\\.*\\w+)+)\\}$");

    public static List<String> checkForSystemVariables(List<String> defaultValues) {
        if (defaultValues.isEmpty())
            return defaultValues;
        // Fast path: skip regex if no values look like system variable references
        boolean hasCandidate = false;
        for (String v : defaultValues) {
            if (v.length() > 3 && v.charAt(0) == '$' && v.charAt(1) == '{') {
                hasCandidate = true;
                break;
            }
        }
        if (!hasCandidate)
            return defaultValues;

        List<String> result = new ArrayList<>(defaultValues.size());
        for (String v : defaultValues) {
            if (v.length() > 3 && v.charAt(0) == '$' && v.charAt(1) == '{') {
                Matcher m = systemProperties.matcher(v);
                if (m.matches()) {
                    //handle env:
                    if (m.group(2) != null) {
                        result.add(findEnvironmentVariable(m.group(4)));
                    }
                    // handle sys:
                    else if (m.group(3) != null) {
                        result.add(findSystemProperty(m.group(4)));
                    }
                    // try to search for all
                    else if (m.group(4) != null) {
                        String value = findSystemProperty(m.group(4));
                        if (value == null)
                            value = findEnvironmentVariable(m.group(4));
                        result.add(value);
                    }
                } else {
                    result.add(v);
                }
            } else {
                result.add(v);
            }
        }

        return result;
    }

    public static String findEnvironmentVariable(String variable) {
        try {
            String value = System.getenv(variable);
            return value != null ? value : "";
        } catch (SecurityException e) {
            // SecurityException if access to environment variables is restricted
            return "";
        }
    }

    public static String findSystemProperty(String key) {
        try {
            return System.getProperty(key);
        } catch (SecurityException e) {
            // SecurityException if access to system properties is restricted
            return "";
        }
    }

}
