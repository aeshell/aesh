package org.aesh.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves variable references in annotation string values.
 * <p>
 * Supports:
 * <ul>
 * <li><code>${env:VAR}</code> — environment variable lookup</li>
 * <li><code>${sys:prop}</code> — system property lookup</li>
 * <li><code>${key}</code> — try system property first, then environment variable</li>
 * <li><code>${key:-fallback}</code> — use fallback if key is not found (bash-style)</li>
 * <li><code>${env:A:-${sys:B:-default}}</code> — nested fallback chains</li>
 * <li><code>$${...}</code> — escape, produces literal <code>${...}</code></li>
 * </ul>
 * <p>
 * "Not found" means the variable does not exist (null from the source).
 * A variable set to empty string is considered found and is used as-is.
 */
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

    /** Maximum nesting depth for recursive fallback resolution. */
    private static final int MAX_DEPTH = 10;

    /**
     * Resolve a single value that may contain a variable reference.
     * <p>
     * Supports <code>${env:VAR}</code>, <code>${sys:prop}</code>, <code>${key}</code>,
     * the <code>:-</code> fallback syntax, nested expressions, and <code>$$</code> escaping.
     * <p>
     * Zero-allocation for non-variable strings (fast-path returns immediately).
     *
     * @param value the value to resolve
     * @return the resolved value, or the original string if not a variable reference
     */
    public static String resolveVariable(String value) {
        return resolveWithSource(value, 0).value;
    }

    /**
     * Resolve a variable reference and report whether the primary variable
     * (before the <code>:-</code> fallback) was found.
     * <p>
     * This is used to determine resolution priority: when the primary
     * variable resolved (e.g., env var was set), the resolved value should
     * take priority over a {@code DefaultValueProvider} (#521).
     *
     * @param value the value to resolve
     * @return a {@link ResolvedValue} with the resolved string and source flag
     */
    public static ResolvedValue resolveWithSource(String value) {
        return resolveWithSource(value, 0);
    }

    private static ResolvedValue resolveWithSource(String value, int depth) {
        if (value == null || value.length() <= 3)
            return new ResolvedValue(value, false);

        if (depth >= MAX_DEPTH)
            return new ResolvedValue(value, false);

        // Escape: $${...} produces literal ${...}
        if (value.length() > 4 && value.charAt(0) == '$' && value.charAt(1) == '$'
                && value.charAt(2) == '{') {
            return new ResolvedValue(value.substring(1), false);
        }

        if (value.charAt(0) != '$' || value.charAt(1) != '{')
            return new ResolvedValue(value, false);

        // Find the matching closing brace (handles nesting)
        int closeBrace = findMatchingBrace(value, 1);
        if (closeBrace < 0 || closeBrace != value.length() - 1)
            return new ResolvedValue(value, false);

        // Extract content between ${ and }
        String content = value.substring(2, closeBrace);
        if (content.isEmpty())
            return new ResolvedValue(value, false);

        return resolveExpression(content, depth);
    }

    /**
     * Resolve the content of a <code>${...}</code> expression.
     * The content may contain a <code>:-</code> fallback separator with
     * optional nested <code>${...}</code> expressions in the fallback part.
     */
    private static ResolvedValue resolveExpression(String content, int depth) {
        // Find the :- separator at the top level (not inside nested ${...})
        int fallbackIdx = findFallbackSeparator(content);

        String varPart;
        String fallback;
        if (fallbackIdx >= 0) {
            varPart = content.substring(0, fallbackIdx);
            fallback = content.substring(fallbackIdx + 2); // skip ":-"
        } else {
            varPart = content;
            fallback = null;
        }

        // Resolve the variable name
        String resolved = lookupVariable(varPart);

        // Invalid variable name — return original literal
        if (resolved == INVALID) {
            return new ResolvedValue("${" + content + "}", false);
        }

        if (resolved != null) {
            return new ResolvedValue(resolved, true); // primary variable found
        }

        // Variable not found — use fallback
        if (fallback != null) {
            // The fallback may itself be a variable expression.
            // fromVariable stays false — we didn't resolve the primary variable.
            ResolvedValue fallbackResult = resolveWithSource(fallback, depth + 1);
            return new ResolvedValue(fallbackResult.value, false);
        }

        // No fallback, not found — return empty string for backward compatibility
        return new ResolvedValue("", false);
    }

    /**
     * Result of resolving a variable expression, including whether the
     * primary variable (before <code>:-</code>) was found.
     */
    public static final class ResolvedValue {
        /** The resolved value. */
        public final String value;
        /**
         * True if the primary variable resolved (e.g., env var was set).
         * False if the fallback was used or the input was a literal.
         */
        public final boolean fromVariable;

        public ResolvedValue(String value, boolean fromVariable) {
            this.value = value;
            this.fromVariable = fromVariable;
        }
    }

    /**
     * Look up a variable by name, respecting env:/sys: prefixes.
     * <p>
     * Returns a two-state result via a special sentinel: if the variable name
     * is structurally invalid (e.g., empty after prefix), returns {@link #INVALID}
     * to signal the caller should return the original literal.
     *
     * @return the resolved value, null if not found, or INVALID if malformed
     */
    private static final String INVALID = new String("\0INVALID");

    private static String lookupVariable(String varPart) {
        if (varPart.isEmpty())
            return INVALID;

        if (varPart.startsWith("env:")) {
            String name = varPart.substring(4);
            return name.isEmpty() ? INVALID : findEnvironmentVariable(name);
        }

        if (varPart.startsWith("sys:")) {
            String name = varPart.substring(4);
            return name.isEmpty() ? INVALID : findSystemProperty(name);
        }

        // No prefix — try system property first, then environment variable
        String resolved = findSystemProperty(varPart);
        if (resolved != null)
            return resolved;
        return findEnvironmentVariable(varPart);
    }

    /**
     * Find the index of the <code>:-</code> fallback separator at the top level,
     * skipping any nested <code>${...}</code> expressions.
     *
     * @return the index of the colon in <code>:-</code>, or -1 if not found
     */
    private static int findFallbackSeparator(String content) {
        int depth = 0;
        for (int i = 0; i < content.length() - 1; i++) {
            char c = content.charAt(i);
            if (c == '$' && i + 1 < content.length() && content.charAt(i + 1) == '{') {
                depth++;
                i++; // skip '{'
            } else if (c == '}') {
                depth--;
            } else if (depth == 0 && c == ':' && content.charAt(i + 1) == '-') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the matching closing brace for a <code>${</code> at the given position,
     * accounting for nested <code>${...}</code> pairs.
     *
     * @param value the full string
     * @param openBrace the index of the opening brace
     * @return the index of the matching closing brace, or -1 if not found
     */
    private static int findMatchingBrace(String value, int openBrace) {
        int depth = 1;
        for (int i = openBrace + 1; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '$' && i + 1 < value.length() && value.charAt(i + 1) == '{') {
                depth++;
                i++; // skip '{'
            } else if (c == '}') {
                depth--;
                if (depth == 0)
                    return i;
            }
        }
        return -1;
    }

    /**
     * Look up an environment variable.
     *
     * @return the value, or null if the variable is not set
     */
    public static String findEnvironmentVariable(String variable) {
        try {
            return System.getenv(variable);
        } catch (SecurityException e) {
            return null;
        }
    }

    /**
     * Look up a system property.
     *
     * @return the value, or null if the property is not set
     */
    public static String findSystemProperty(String key) {
        try {
            return System.getProperty(key);
        } catch (SecurityException e) {
            return null;
        }
    }

}
