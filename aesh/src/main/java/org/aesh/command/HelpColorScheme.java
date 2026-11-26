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
package org.aesh.command;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.aesh.terminal.utils.ANSI;

/**
 * Color and text-attribute scheme for {@code --help} and {@code Usage:}
 * rendering.
 * <p>
 * Covers five roles:
 * <ul>
 * <li>{@code commands}: command name on the {@code Usage:} line and
 * sub-command names in group listings (default: bold)</li>
 * <li>{@code options}: option names, grouped short flags ({@code [-hV]}),
 * and {@code OPTIONS} in {@code [OPTIONS]} (default: yellow)</li>
 * <li>{@code parameters}: positional argument labels such as
 * {@code <file>} (default: cyan)</li>
 * <li>{@code optionParams}: option value placeholders such as
 * {@code =<file>} and {@code <key>=<value>} (default: cyan)</li>
 * <li>{@code required}: extra style merged onto required options and
 * required positional arguments in a single SGR sequence (default: bold)</li>
 * </ul>
 * <p>
 * Applications can set a scheme per parser
 * ({@code CommandLineParser.updateHelpColorScheme}), via
 * {@code AeshRuntimeRunner.helpColorScheme}, via
 * {@code AeshCommandRegistryBuilder.helpColorScheme}, or process-wide with
 * {@link #setDefault(HelpColorScheme)}. End users can override individual
 * slots through the {@code AESH_HELP_COLORS} environment variable
 * (colon-separated {@code key=value} entries, e.g.
 * {@code options=blue:required=bold,red}) or the {@code aesh.help.color.*}
 * system properties. {@code NO_COLOR} and {@code updateAnsiMode(false)}
 * always suppress all ANSI escapes regardless of the scheme.
 *
 * @author Aesh team
 */
public final class HelpColorScheme {

    /** Environment variable for end-user help color overrides ({@code key=value:...}). */
    public static final String ENV_HELP_COLORS = "AESH_HELP_COLORS";
    /** System property overriding the {@code commands} help style. */
    public static final String PROP_COMMANDS = "aesh.help.color.commands";
    /** System property overriding the {@code options} help style. */
    public static final String PROP_OPTIONS = "aesh.help.color.options";
    /** System property overriding the {@code parameters} help style. */
    public static final String PROP_PARAMETERS = "aesh.help.color.parameters";
    /** System property overriding the {@code optionParams} help style. */
    public static final String PROP_OPTION_PARAMS = "aesh.help.color.optionParams";
    /** System property overriding the {@code required} help style. */
    public static final String PROP_REQUIRED = "aesh.help.color.required";

    private static final int ATTR_BOLD = 1;
    private static final int ATTR_FAINT = 1 << 1;
    private static final int ATTR_ITALIC = 1 << 2;
    private static final int ATTR_UNDERLINE = 1 << 3;
    private static final int ATTR_BLINK = 1 << 4;
    private static final int ATTR_REVERSE = 1 << 5;

    /**
     * Built-in text attributes and 16-color ANSI palette entries.
     */
    public enum Style {
        /** No styling (clears a slot when used alone). */
        NONE(SlotKind.NONE, 0, null),

        BOLD(SlotKind.ATTR, ATTR_BOLD, "1"),
        FAINT(SlotKind.ATTR, ATTR_FAINT, "2"),
        ITALIC(SlotKind.ATTR, ATTR_ITALIC, "3"),
        UNDERLINE(SlotKind.ATTR, ATTR_UNDERLINE, "4"),
        BLINK(SlotKind.ATTR, ATTR_BLINK, "5"),
        REVERSE(SlotKind.ATTR, ATTR_REVERSE, "7"),

        BLACK(SlotKind.FG, 0, "30"),
        RED(SlotKind.FG, 0, "31"),
        GREEN(SlotKind.FG, 0, "32"),
        YELLOW(SlotKind.FG, 0, "33"),
        BLUE(SlotKind.FG, 0, "34"),
        MAGENTA(SlotKind.FG, 0, "35"),
        CYAN(SlotKind.FG, 0, "36"),
        WHITE(SlotKind.FG, 0, "37"),
        DEFAULT(SlotKind.FG, 0, "39"),

        BRIGHT_BLACK(SlotKind.FG, 0, "90"),
        BRIGHT_RED(SlotKind.FG, 0, "91"),
        BRIGHT_GREEN(SlotKind.FG, 0, "92"),
        BRIGHT_YELLOW(SlotKind.FG, 0, "93"),
        BRIGHT_BLUE(SlotKind.FG, 0, "94"),
        BRIGHT_MAGENTA(SlotKind.FG, 0, "95"),
        BRIGHT_CYAN(SlotKind.FG, 0, "96"),
        BRIGHT_WHITE(SlotKind.FG, 0, "97"),

        BG_BLACK(SlotKind.BG, 0, "40"),
        BG_RED(SlotKind.BG, 0, "41"),
        BG_GREEN(SlotKind.BG, 0, "42"),
        BG_YELLOW(SlotKind.BG, 0, "43"),
        BG_BLUE(SlotKind.BG, 0, "44"),
        BG_MAGENTA(SlotKind.BG, 0, "45"),
        BG_CYAN(SlotKind.BG, 0, "46"),
        BG_WHITE(SlotKind.BG, 0, "47"),
        BG_DEFAULT(SlotKind.BG, 0, "49"),

        BG_BRIGHT_BLACK(SlotKind.BG, 0, "100"),
        BG_BRIGHT_RED(SlotKind.BG, 0, "101"),
        BG_BRIGHT_GREEN(SlotKind.BG, 0, "102"),
        BG_BRIGHT_YELLOW(SlotKind.BG, 0, "103"),
        BG_BRIGHT_BLUE(SlotKind.BG, 0, "104"),
        BG_BRIGHT_MAGENTA(SlotKind.BG, 0, "105"),
        BG_BRIGHT_CYAN(SlotKind.BG, 0, "106"),
        BG_BRIGHT_WHITE(SlotKind.BG, 0, "107");

        final SlotKind kind;
        final int attrBit;
        final String sgr;

        Style(SlotKind kind, int attrBit, String sgr) {
            this.kind = kind;
            this.attrBit = attrBit;
            this.sgr = sgr;
        }
    }

    private enum SlotKind {
        NONE,
        ATTR,
        FG,
        BG
    }

    private static final Map<String, Style> NAMED_STYLES = buildStyleIndex();

    private static final SlotStyle EMPTY_SLOT = new SlotStyle(0, null, null);
    private static final SlotStyle DEFAULT_COMMANDS = SlotStyle.of(Style.BOLD);
    private static final SlotStyle DEFAULT_OPTIONS = SlotStyle.of(Style.YELLOW);
    private static final SlotStyle DEFAULT_PARAMETERS = SlotStyle.of(Style.CYAN);
    private static final SlotStyle DEFAULT_OPTION_PARAMS = SlotStyle.of(Style.CYAN);
    private static final SlotStyle DEFAULT_REQUIRED = SlotStyle.of(Style.BOLD);

    private static final HelpColorScheme DEFAULTS = new HelpColorScheme(
            DEFAULT_COMMANDS, DEFAULT_OPTIONS, DEFAULT_PARAMETERS, DEFAULT_OPTION_PARAMS, DEFAULT_REQUIRED);
    private static final HelpColorScheme PLAIN = new HelpColorScheme(
            EMPTY_SLOT, EMPTY_SLOT, EMPTY_SLOT, EMPTY_SLOT, EMPTY_SLOT);

    private static volatile HelpColorScheme defaultScheme = DEFAULTS;

    private final SlotStyle commands;
    private final SlotStyle options;
    private final SlotStyle parameters;
    private final SlotStyle optionParams;
    private final SlotStyle required;
    private final String requiredOptionEscape;
    private final String requiredParameterEscape;

    private HelpColorScheme(SlotStyle commands, SlotStyle options, SlotStyle parameters,
            SlotStyle optionParams, SlotStyle required) {
        this.commands = commands;
        this.options = options;
        this.parameters = parameters;
        this.optionParams = optionParams;
        this.required = required;
        this.requiredOptionEscape = options.merge(required).escape;
        this.requiredParameterEscape = parameters.merge(required).escape;
    }

    /**
     * Built-in default scheme: bold commands, yellow options, cyan
     * parameters and option values, bold merged onto required items.
     */
    public static HelpColorScheme defaults() {
        return DEFAULTS;
    }

    /**
     * Unstyled scheme that emits no ANSI sequences for any slot.
     */
    public static HelpColorScheme plain() {
        return PLAIN;
    }

    /**
     * Returns the process-wide default scheme used when a parser does not
     * specify one. Starts as {@link #defaults()}.
     */
    public static HelpColorScheme getDefault() {
        return defaultScheme;
    }

    /**
     * Sets the process-wide default scheme used when a parser does not
     * specify one. Passing {@code null} restores {@link #defaults()}.
     */
    public static void setDefault(HelpColorScheme scheme) {
        defaultScheme = scheme != null ? scheme : DEFAULTS;
    }

    /**
     * Starts a new builder seeded with {@link #defaults()}.
     */
    public static Builder builder() {
        return new Builder(DEFAULTS);
    }

    /**
     * Starts a new builder seeded with {@code base} (or {@link #defaults()}
     * if {@code base} is null).
     */
    public static Builder builder(HelpColorScheme base) {
        return new Builder(base != null ? base : DEFAULTS);
    }

    /**
     * Resolves the effective scheme for rendering: starts from {@code base}
     * (or {@link #getDefault()} when null), then layers {@code AESH_HELP_COLORS}
     * and {@code aesh.help.color.*} system properties on top. Returns
     * {@code base} directly without allocating when no overrides are set.
     */
    public static HelpColorScheme resolve(HelpColorScheme base) {
        HelpColorScheme effective = base != null ? base : defaultScheme;
        String envSpec = System.getenv(ENV_HELP_COLORS);
        String pCmd = System.getProperty(PROP_COMMANDS);
        String pOpt = System.getProperty(PROP_OPTIONS);
        String pParam = System.getProperty(PROP_PARAMETERS);
        String pOptParam = System.getProperty(PROP_OPTION_PARAMS);
        String pReq = System.getProperty(PROP_REQUIRED);
        if (isEmpty(envSpec) && isEmpty(pCmd) && isEmpty(pOpt)
                && isEmpty(pParam) && isEmpty(pOptParam) && isEmpty(pReq)) {
            return effective;
        }
        return applyOverrides(effective, envSpec, pCmd, pOpt, pParam, pOptParam, pReq);
    }

    /**
     * Resolves the effective scheme with injectable environment and system
     * property lookups so tests can exercise precedence without mutating
     * process state.
     */
    public static HelpColorScheme resolve(HelpColorScheme base,
            Function<String, String> getenv, Function<String, String> getProperty) {
        HelpColorScheme effective = base != null ? base : defaultScheme;
        String envSpec = getenv != null ? getenv.apply(ENV_HELP_COLORS) : null;
        String pCmd = getProperty != null ? getProperty.apply(PROP_COMMANDS) : null;
        String pOpt = getProperty != null ? getProperty.apply(PROP_OPTIONS) : null;
        String pParam = getProperty != null ? getProperty.apply(PROP_PARAMETERS) : null;
        String pOptParam = getProperty != null ? getProperty.apply(PROP_OPTION_PARAMS) : null;
        String pReq = getProperty != null ? getProperty.apply(PROP_REQUIRED) : null;
        if (isEmpty(envSpec) && isEmpty(pCmd) && isEmpty(pOpt)
                && isEmpty(pParam) && isEmpty(pOptParam) && isEmpty(pReq)) {
            return effective;
        }
        return applyOverrides(effective, envSpec, pCmd, pOpt, pParam, pOptParam, pReq);
    }

    private static HelpColorScheme applyOverrides(HelpColorScheme base, String envSpec,
            String pCmd, String pOpt, String pParam, String pOptParam, String pReq) {
        SlotStyle cmd = base.commands;
        SlotStyle opt = base.options;
        SlotStyle param = base.parameters;
        SlotStyle optParam = base.optionParams;
        SlotStyle req = base.required;

        if (!isEmpty(envSpec)) {
            SlotStyle[] slots = new SlotStyle[] { cmd, opt, param, optParam, req };
            applyEnvSpec(envSpec, slots);
            cmd = slots[0];
            opt = slots[1];
            param = slots[2];
            optParam = slots[3];
            req = slots[4];
        }

        cmd = parseLenientOrKeep(pCmd, cmd);
        opt = parseLenientOrKeep(pOpt, opt);
        param = parseLenientOrKeep(pParam, param);
        optParam = parseLenientOrKeep(pOptParam, optParam);
        req = parseLenientOrKeep(pReq, req);

        if (cmd == base.commands && opt == base.options && param == base.parameters
                && optParam == base.optionParams && req == base.required) {
            return base;
        }
        return new HelpColorScheme(cmd, opt, param, optParam, req);
    }

    private static SlotStyle parseLenientOrKeep(String spec, SlotStyle current) {
        if (spec == null) {
            return current;
        }
        SlotStyle parsed = parseSlot(spec, false);
        return parsed != null ? parsed : current;
    }

    private static void applyEnvSpec(String envSpec, SlotStyle[] slots) {
        int start = 0;
        int len = envSpec.length();
        while (start < len) {
            int colon = envSpec.indexOf(':', start);
            int end = colon < 0 ? len : colon;
            String entry = envSpec.substring(start, end).trim();
            int eq = entry.indexOf('=');
            if (eq > 0) {
                String key = entry.substring(0, eq).trim().toLowerCase(Locale.ROOT);
                String val = entry.substring(eq + 1).trim();
                int idx = slotIndex(key);
                if (idx >= 0) {
                    SlotStyle parsed = parseSlot(val, false);
                    if (parsed != null) {
                        slots[idx] = parsed;
                    }
                }
            }
            start = end + 1;
        }
    }

    private static int slotIndex(String key) {
        switch (key) {
            case "command":
            case "commands":
                return 0;
            case "option":
            case "options":
                return 1;
            case "parameter":
            case "parameters":
                return 2;
            case "optionparam":
            case "optionparams":
            case "option-params":
            case "option_params":
                return 3;
            case "required":
                return 4;
            default:
                return -1;
        }
    }

    /** ANSI SGR opening escape for command names (empty when unstyled). */
    public String commandsEscape() {
        return commands.escape;
    }

    /** ANSI SGR opening escape for option names (empty when unstyled). */
    public String optionsEscape() {
        return options.escape;
    }

    /** ANSI SGR opening escape for positional argument labels (empty when unstyled). */
    public String parametersEscape() {
        return parameters.escape;
    }

    /** ANSI SGR opening escape for option value placeholders (empty when unstyled). */
    public String optionParamsEscape() {
        return optionParams.escape;
    }

    /** ANSI SGR opening escape for the required overlay alone (empty when unstyled). */
    public String requiredEscape() {
        return required.escape;
    }

    /** Merged single-sequence SGR escape for required option names. */
    public String requiredOptionEscape() {
        return requiredOptionEscape;
    }

    /** Merged single-sequence SGR escape for required positional labels. */
    public String requiredParameterEscape() {
        return requiredParameterEscape;
    }

    /** Wraps {@code text} with the {@code commands} style and a reset. */
    public String styleCommand(String text) {
        return wrap(commands.escape, text);
    }

    /** Wraps {@code text} with the {@code options} style and a reset. */
    public String styleOption(String text) {
        return wrap(options.escape, text);
    }

    /** Wraps {@code text} with the merged required-option style and a reset. */
    public String styleRequiredOption(String text) {
        return wrap(requiredOptionEscape, text);
    }

    /** Wraps {@code text} with the {@code parameters} style and a reset. */
    public String styleParameter(String text) {
        return wrap(parameters.escape, text);
    }

    /** Wraps {@code text} with the merged required-parameter style and a reset. */
    public String styleRequiredParameter(String text) {
        return wrap(requiredParameterEscape, text);
    }

    /** Wraps {@code text} with the {@code optionParams} style and a reset. */
    public String styleOptionParam(String text) {
        return wrap(optionParams.escape, text);
    }

    /** Appends {@code text} wrapped with {@code escape} and reset when non-empty. */
    public static void appendStyled(StringBuilder sb, String escape, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (escape != null && !escape.isEmpty()) {
            sb.append(escape).append(text).append(ANSI.RESET);
        } else {
            sb.append(text);
        }
    }

    private static String wrap(String escape, String text) {
        if (text == null || text.isEmpty() || escape.isEmpty()) {
            return text;
        }
        return escape + text + ANSI.RESET;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Fluent builder for {@link HelpColorScheme}.
     */
    public static final class Builder {
        private SlotStyle commands;
        private SlotStyle options;
        private SlotStyle parameters;
        private SlotStyle optionParams;
        private SlotStyle required;

        private Builder(HelpColorScheme base) {
            this.commands = base.commands;
            this.options = base.options;
            this.parameters = base.parameters;
            this.optionParams = base.optionParams;
            this.required = base.required;
        }

        public Builder commands(Style... styles) {
            this.commands = SlotStyle.of(styles);
            return this;
        }

        public Builder commands(String spec) {
            this.commands = parseSlot(spec, true);
            return this;
        }

        public Builder options(Style... styles) {
            this.options = SlotStyle.of(styles);
            return this;
        }

        public Builder options(String spec) {
            this.options = parseSlot(spec, true);
            return this;
        }

        public Builder parameters(Style... styles) {
            this.parameters = SlotStyle.of(styles);
            return this;
        }

        public Builder parameters(String spec) {
            this.parameters = parseSlot(spec, true);
            return this;
        }

        public Builder optionParams(Style... styles) {
            this.optionParams = SlotStyle.of(styles);
            return this;
        }

        public Builder optionParams(String spec) {
            this.optionParams = parseSlot(spec, true);
            return this;
        }

        public Builder required(Style... styles) {
            this.required = SlotStyle.of(styles);
            return this;
        }

        public Builder required(String spec) {
            this.required = parseSlot(spec, true);
            return this;
        }

        public HelpColorScheme build() {
            return new HelpColorScheme(commands, options, parameters, optionParams, required);
        }
    }

    private static final class SlotStyle {
        final int attrs;
        final String fg;
        final String bg;
        final String escape;

        SlotStyle(int attrs, String fg, String bg) {
            this.attrs = attrs;
            this.fg = fg;
            this.bg = bg;
            this.escape = buildEscape(attrs, fg, bg);
        }

        static SlotStyle of(Style... styles) {
            if (styles == null || styles.length == 0) {
                return EMPTY_SLOT;
            }
            int attrs = 0;
            String fg = null;
            String bg = null;
            for (Style style : styles) {
                if (style == null || style.kind == SlotKind.NONE) {
                    continue;
                }
                if (style.kind == SlotKind.ATTR) {
                    attrs |= style.attrBit;
                } else if (style.kind == SlotKind.FG) {
                    fg = style.sgr;
                } else if (style.kind == SlotKind.BG) {
                    bg = style.sgr;
                }
            }
            return (attrs == 0 && fg == null && bg == null) ? EMPTY_SLOT : new SlotStyle(attrs, fg, bg);
        }

        SlotStyle merge(SlotStyle overlay) {
            if (overlay == EMPTY_SLOT) {
                return this;
            }
            if (this == EMPTY_SLOT) {
                return overlay;
            }
            int mergedAttrs = this.attrs | overlay.attrs;
            String mergedFg = overlay.fg != null ? overlay.fg : this.fg;
            String mergedBg = overlay.bg != null ? overlay.bg : this.bg;
            return new SlotStyle(mergedAttrs, mergedFg, mergedBg);
        }

        private static String buildEscape(int attrs, String fg, String bg) {
            if (attrs == 0 && fg == null && bg == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder(16);
            sb.append(ANSI.START);
            boolean sep = false;
            sep = appendAttr(sb, attrs, ATTR_BOLD, "1", sep);
            sep = appendAttr(sb, attrs, ATTR_FAINT, "2", sep);
            sep = appendAttr(sb, attrs, ATTR_ITALIC, "3", sep);
            sep = appendAttr(sb, attrs, ATTR_UNDERLINE, "4", sep);
            sep = appendAttr(sb, attrs, ATTR_BLINK, "5", sep);
            sep = appendAttr(sb, attrs, ATTR_REVERSE, "7", sep);
            if (fg != null) {
                if (sep) {
                    sb.append(';');
                }
                sb.append(fg);
                sep = true;
            }
            if (bg != null) {
                if (sep) {
                    sb.append(';');
                }
                sb.append(bg);
            }
            sb.append('m');
            return sb.toString();
        }

        private static boolean appendAttr(StringBuilder sb, int attrs, int bit, String code, boolean sep) {
            if ((attrs & bit) == 0) {
                return sep;
            }
            if (sep) {
                sb.append(';');
            }
            sb.append(code);
            return true;
        }
    }

    private static SlotStyle parseSlot(String spec, boolean strict) {
        if (spec == null) {
            return EMPTY_SLOT;
        }
        String trimmed = spec.trim();
        if (trimmed.isEmpty()) {
            return EMPTY_SLOT;
        }
        int[] attrs = new int[1];
        String[] fgBg = new String[2];
        int start = 0;
        int len = trimmed.length();
        while (start <= len) {
            int comma = trimmed.indexOf(',', start);
            int end = comma < 0 ? len : comma;
            String token = trimmed.substring(start, end).trim();
            if (!token.isEmpty() && !applyToken(token, attrs, fgBg)) {
                if (strict) {
                    throw new IllegalArgumentException("Invalid help style token: '" + token + "'");
                }
                return null;
            }
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
        return (attrs[0] == 0 && fgBg[0] == null && fgBg[1] == null)
                ? EMPTY_SLOT
                : new SlotStyle(attrs[0], fgBg[0], fgBg[1]);
    }

    private static boolean applyToken(String raw, int[] attrs, String[] fgBg) {
        String lower = raw.toLowerCase(Locale.ROOT);
        if ("none".equals(lower) || "plain".equals(lower)) {
            attrs[0] = 0;
            fgBg[0] = null;
            fgBg[1] = null;
            return true;
        }
        if (lower.startsWith("fg(") && lower.endsWith(")")) {
            return setColorSlot(fgBg, 0, lower.substring(3, lower.length() - 1).trim(), false);
        }
        if (lower.startsWith("bg(") && lower.endsWith(")")) {
            return setColorSlot(fgBg, 1, lower.substring(3, lower.length() - 1).trim(), true);
        }
        if (lower.startsWith("#")) {
            return setColorSlot(fgBg, 0, lower, false);
        }
        return applyNamedStyle(lower.replace('-', '_'), attrs, fgBg);
    }

    private static boolean setColorSlot(String[] fgBg, int index, String arg, boolean background) {
        String code = parseColorArg(arg, background);
        if (code == null) {
            return false;
        }
        fgBg[index] = code;
        return true;
    }

    private static boolean applyNamedStyle(String normalized, int[] attrs, String[] fgBg) {
        Style style = NAMED_STYLES.get(normalized);
        if (style == null) {
            return false;
        }
        if (style.kind == SlotKind.ATTR) {
            attrs[0] |= style.attrBit;
        } else if (style.kind == SlotKind.FG) {
            fgBg[0] = style.sgr;
        } else if (style.kind == SlotKind.BG) {
            fgBg[1] = style.sgr;
        }
        return true;
    }

    private static String parseColorArg(String arg, boolean background) {
        if (arg.isEmpty()) {
            return null;
        }
        if (arg.startsWith("#")) {
            return parseHexColor(arg, background);
        }
        if (arg.length() <= 3 && isDigits(arg)) {
            int idx = Integer.parseInt(arg);
            return idx <= 255 ? (background ? "48;5;" : "38;5;") + idx : null;
        }
        String normalized = arg.replace('-', '_');
        String lookup = background && !normalized.startsWith("bg_") ? "bg_" + normalized : normalized;
        Style style = NAMED_STYLES.get(lookup);
        SlotKind expected = background ? SlotKind.BG : SlotKind.FG;
        return style != null && style.kind == expected ? style.sgr : null;
    }

    private static String parseHexColor(String hex, boolean background) {
        if (hex.length() != 7 || hex.charAt(0) != '#') {
            return null;
        }
        int r = parseHexByte(hex, 1);
        int g = parseHexByte(hex, 3);
        int b = parseHexByte(hex, 5);
        if (r < 0 || g < 0 || b < 0) {
            return null;
        }
        return (background ? "48;2;" : "38;2;") + r + ";" + g + ";" + b;
    }

    private static int parseHexByte(String hex, int offset) {
        int hi = Character.digit(hex.charAt(offset), 16);
        int lo = Character.digit(hex.charAt(offset + 1), 16);
        if (hi < 0 || lo < 0) {
            return -1;
        }
        return (hi << 4) | lo;
    }

    private static boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Style> buildStyleIndex() {
        Map<String, Style> map = new HashMap<>(64);
        for (Style style : Style.values()) {
            map.put(style.name().toLowerCase(Locale.ROOT), style);
        }
        map.put("dim", Style.FAINT);
        map.put("inverse", Style.REVERSE);
        map.put("gray", Style.BRIGHT_BLACK);
        map.put("grey", Style.BRIGHT_BLACK);
        map.put("bg_gray", Style.BG_BRIGHT_BLACK);
        map.put("bg_grey", Style.BG_BRIGHT_BLACK);
        return map;
    }
}
