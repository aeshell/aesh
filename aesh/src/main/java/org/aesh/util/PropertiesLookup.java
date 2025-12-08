package org.aesh.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertiesLookup {
    public static final Pattern systemProperties = Pattern.compile("^\\$\\{((env:)|(sys:))?((\\.*\\w+)+)\\}$");

    public static List<String> checkForSystemVariables(List<String> defaultValues) {
        List<String> result = new ArrayList<>(defaultValues.size());
        for (String v : defaultValues) {
            Matcher m = systemProperties.matcher(v);
            if (m.matches()) {
                //handle env:
                if(m.group(2) != null) {
                    result.add(findEnvironmentVariable(m.group(4)));
                }
                // handle sys:
                else if(m.group(3) != null) {
                    result.add(findSystemProperty(m.group(4)));
                }
                // try to search for all
                else if(m.group(4) != null) {
                    String value = findSystemProperty(m.group(4));
                    if(value == null)
                        value = findEnvironmentVariable(m.group(4));
                    result.add(value);
                }
            }
            else {
                result.add(v);
            }
        }

        return result;
    }

    public static String findEnvironmentVariable(String variable) {
        try {
            String value = System.getenv(variable);
            return value != null ? value : "";
        }
        catch(Exception e) {
            // System.getenv will throw an exception if a key is not found, lets just return empty string
            return "";
        }
    }

    public static String findSystemProperty(String key) {
        try {
            return System.getProperty(key);
        } catch (Exception e) {
            // System.getProperties will throw an exception if a key is not found, lets just return empty string
            return "";
        }
    }

}
