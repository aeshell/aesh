package org.aesh.util;

import java.util.ArrayList;
import java.util.List;

public class PropertiesLookup {

    public static List<String> checkForSystemVariables(List<String> defaultValues) {
        if (defaultValues.isEmpty())
            return defaultValues;
        // Fast path: skip if no values look like variable references
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
     * <p>
     * Zero-allocation for non-variable strings (fast-path returns immediately).
     * For variable strings, uses simple string operations instead of regex.
     *
     * @param value the value to resolve
     * @return the resolved value
     */
    public static String resolveVariable(String value) {
        if (value == null || value.length() <= 3 || value.charAt(0) != '$' || value.charAt(1) != '{')
            return value;

        // Must end with '}'
        if (value.charAt(value.length() - 1) != '}')
            return value;

        // Extract content between ${ and }
        String content = value.substring(2, value.length() - 1);
        if (content.isEmpty())
            return value;

        // Check for env: prefix
        if (content.startsWith("env:")) {
            String varName = content.substring(4);
            return varName.isEmpty() ? value : findEnvironmentVariable(varName);
        }

        // Check for sys: prefix
        if (content.startsWith("sys:")) {
            String propName = content.substring(4);
            return propName.isEmpty() ? value : findSystemProperty(propName);
        }

        // No prefix — try system property first, then environment variable
        String resolved = findSystemProperty(content);
        if (resolved == null)
            resolved = findEnvironmentVariable(content);
        return resolved;
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
