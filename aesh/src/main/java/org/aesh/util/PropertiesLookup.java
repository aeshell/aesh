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
            result.add(resolveVariable(v));
        }

        return result;
    }

    /**
     * Resolve a single value that may contain a {@code ${env:VAR}},
     * {@code ${sys:prop}}, or {@code ${prop}} reference.
     * Returns the resolved value, or the original string if it is not
     * a variable reference.
     *
     * @param value the value to resolve
     * @return the resolved value
     */
    public static String resolveVariable(String value) {
        if (value == null || value.length() <= 3 || value.charAt(0) != '$' || value.charAt(1) != '{')
            return value;

        Matcher m = systemProperties.matcher(value);
        if (m.matches()) {
            //handle env:
            if (m.group(2) != null) {
                return findEnvironmentVariable(m.group(4));
            }
            // handle sys:
            else if (m.group(3) != null) {
                return findSystemProperty(m.group(4));
            }
            // try to search for all
            else if (m.group(4) != null) {
                String resolved = findSystemProperty(m.group(4));
                if (resolved == null)
                    resolved = findEnvironmentVariable(m.group(4));
                return resolved;
            }
        }
        return value;
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
