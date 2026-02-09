/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag
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
package org.aesh.command.impl.context;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aesh.command.Command;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.settings.SubCommandModeSettings;

/**
 * Tracks the current command context state for sub-command mode.
 * Maintains a stack of context frames, each representing a group command
 * that has been entered. Provides access to parent command values.
 *
 * @author Aesh team
 */
public class CommandContext {
    private final Deque<ContextFrame> contextStack;
    private final String originalPrompt;
    private SubCommandModeSettings settings;

    public CommandContext(String originalPrompt) {
        this(originalPrompt, SubCommandModeSettings.defaults());
    }

    public CommandContext(String originalPrompt, SubCommandModeSettings settings) {
        this.contextStack = new ArrayDeque<>();
        this.originalPrompt = originalPrompt;
        this.settings = settings != null ? settings : SubCommandModeSettings.defaults();
    }

    /**
     * Get the sub-command mode settings.
     *
     * @return the settings
     */
    public SubCommandModeSettings getSettings() {
        return settings;
    }

    /**
     * Set the sub-command mode settings.
     *
     * @param settings the settings
     */
    public void setSettings(SubCommandModeSettings settings) {
        this.settings = settings != null ? settings : SubCommandModeSettings.defaults();
    }

    /**
     * Push a new context frame when entering a group command.
     *
     * @param parser The group command's parser
     * @param command The populated command instance with parsed values
     */
    public void push(CommandLineParser<?> parser, Command<?> command) {
        contextStack.push(new ContextFrame(parser, command));
    }

    /**
     * Pop the current context frame when exiting.
     *
     * @return the popped context frame
     */
    public ContextFrame pop() {
        return contextStack.pop();
    }

    /**
     * Get the current context frame.
     *
     * @return the current frame, or null if not in sub-command mode
     */
    public ContextFrame current() {
        return contextStack.peek();
    }

    /**
     * Check if we're in sub-command mode.
     *
     * @return true if in sub-command mode
     */
    public boolean isInSubCommandMode() {
        return !contextStack.isEmpty();
    }

    /**
     * Get the depth of the context stack.
     *
     * @return the number of nested contexts
     */
    public int depth() {
        return contextStack.size();
    }

    /**
     * Get the context path as a string (e.g., "module:project").
     *
     * @return the context path
     */
    public String getContextPath() {
        List<String> names = new ArrayList<>();
        for (ContextFrame frame : contextStack) {
            names.add(0, frame.getCommandName());
        }
        return String.join(settings.getContextSeparator(), names);
    }

    /**
     * Get the context path with spaces for command prefixing (e.g., "module project").
     *
     * @return the context path with spaces
     */
    public String getContextPathWithSpaces() {
        List<String> names = new ArrayList<>();
        for (ContextFrame frame : contextStack) {
            names.add(0, frame.getCommandName());
        }
        return String.join(" ", names);
    }

    /**
     * Build a prompt showing the current context.
     * Examples: "module> ", "module[my-module]> ", "module:project> "
     *
     * @param showArgumentInPrompt whether to show primary argument in prompt
     * @return the formatted prompt string
     */
    public String buildPrompt(boolean showArgumentInPrompt) {
        if (contextStack.isEmpty()) {
            return originalPrompt;
        }

        StringBuilder prompt = new StringBuilder();
        List<ContextFrame> frames = new ArrayList<>(contextStack);
        Collections.reverse(frames);

        String separator = settings.getContextSeparator();
        boolean showArg = showArgumentInPrompt && settings.showArgumentInPrompt();

        for (int i = 0; i < frames.size(); i++) {
            if (i > 0) {
                prompt.append(separator);
            }
            ContextFrame frame = frames.get(i);
            prompt.append(frame.getCommandName());

            // Show primary argument value in prompt if present and enabled
            if (showArg) {
                String argValue = frame.getPrimaryArgumentValue();
                if (argValue != null && !argValue.isEmpty()) {
                    prompt.append("[").append(argValue).append("]");
                }
            }
        }
        prompt.append("> ");
        return prompt.toString();
    }

    /**
     * Get the original prompt before entering sub-command mode.
     *
     * @return the original prompt string
     */
    public String getOriginalPrompt() {
        return originalPrompt;
    }

    /**
     * Check if the given command is an exit command for sub-command mode.
     *
     * @param command the command to check
     * @return true if the command should exit sub-command mode
     */
    public boolean isExitCommand(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }
        String trimmed = command.trim();
        if (settings.getExitCommand() != null && trimmed.equals(settings.getExitCommand())) {
            return true;
        }
        if (settings.getAlternativeExitCommand() != null && trimmed.equals(settings.getAlternativeExitCommand())) {
            return true;
        }
        return false;
    }

    /**
     * Format the enter message for sub-command mode.
     *
     * @param commandName the name of the command being entered
     * @return the formatted enter message, or null if no message configured
     */
    public String formatEnterMessage(String commandName) {
        String message = settings.getEnterMessage();
        if (message == null || message.isEmpty()) {
            return null;
        }
        return message.replace("{name}", commandName);
    }

    /**
     * Format the exit hint for sub-command mode.
     *
     * @return the formatted exit hint, or null if no hint configured
     */
    public String formatExitHint() {
        String hint = settings.getExitHint();
        if (hint == null || hint.isEmpty()) {
            return null;
        }
        hint = hint.replace("{exit}", settings.getExitCommand() != null ? settings.getExitCommand() : "exit");
        hint = hint.replace("{alt}",
                settings.getAlternativeExitCommand() != null ? settings.getAlternativeExitCommand() : "..");
        return hint;
    }

    /**
     * Format the exit message for sub-command mode.
     *
     * @param commandName the name of the command being exited
     * @return the formatted exit message, or null if no message configured
     */
    public String formatExitMessage(String commandName) {
        String message = settings.getExitMessage();
        if (message == null || message.isEmpty()) {
            return null;
        }
        return message.replace("{name}", commandName);
    }

    // ========== Parent Value Access Methods ==========

    /**
     * Get a value from any parent command in the context stack.
     * Searches from immediate parent up to root.
     *
     * @param fieldName The field name to look for
     * @param type The expected type
     * @param <T> the value type
     * @return The value, or null if not found
     */
    public <T> T getParentValue(String fieldName, Class<T> type) {
        return getParentValue(fieldName, type, null);
    }

    /**
     * Get a value from any parent command with a default.
     *
     * @param fieldName The field name to look for
     * @param type The expected type
     * @param defaultValue The default value if not found
     * @param <T> the value type
     * @return The value, or defaultValue if not found
     */
    public <T> T getParentValue(String fieldName, Class<T> type, T defaultValue) {
        for (ContextFrame frame : contextStack) {
            Object value = frame.getFieldValue(fieldName);
            if (value != null && type.isInstance(value)) {
                return type.cast(value);
            }
        }
        return defaultValue;
    }

    /**
     * Get the immediate parent command instance.
     *
     * @return the parent command, or null if not in sub-command mode
     */
    public Command<?> getParentCommand() {
        ContextFrame frame = contextStack.peek();
        return frame != null ? frame.getCommand() : null;
    }

    /**
     * Get all parent commands from immediate to root.
     *
     * @return list of parent commands
     */
    public List<Command<?>> getParentCommands() {
        List<Command<?>> parents = new ArrayList<>();
        for (ContextFrame frame : contextStack) {
            parents.add(frame.getCommand());
        }
        return parents;
    }

    /**
     * Get a specific parent command by type.
     *
     * @param type the command class to find
     * @param <T> the command type
     * @return the matching parent command, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends Command<?>> T getParentCommand(Class<T> type) {
        for (ContextFrame frame : contextStack) {
            if (type.isInstance(frame.getCommand())) {
                return (T) frame.getCommand();
            }
        }
        return null;
    }

    // ========== Inherited Value Access Methods ==========

    /**
     * Get an inherited value from parent commands.
     * Only returns values from options marked with inherited=true.
     *
     * @param fieldName The field name to look for
     * @param type The expected type
     * @param <T> the value type
     * @return The inherited value, or null if not found
     */
    public <T> T getInheritedValue(String fieldName, Class<T> type) {
        return getInheritedValue(fieldName, type, null);
    }

    /**
     * Get an inherited value from parent commands with a default.
     * Only returns values from options marked with inherited=true.
     *
     * @param fieldName The field name to look for
     * @param type The expected type
     * @param defaultValue The default value if not found
     * @param <T> the value type
     * @return The inherited value, or defaultValue if not found
     */
    public <T> T getInheritedValue(String fieldName, Class<T> type, T defaultValue) {
        for (ContextFrame frame : contextStack) {
            Object value = frame.getInheritedValue(fieldName);
            if (value != null && type.isInstance(value)) {
                return type.cast(value);
            }
        }
        return defaultValue;
    }

    /**
     * Get all inherited options from the context stack.
     * Returns a map of field names to their values for all inherited options.
     *
     * @return map of inherited field names to values
     */
    public Map<String, Object> getAllInheritedValues() {
        Map<String, Object> inherited = new HashMap<>();
        // Process from root to immediate parent to get correct override order
        List<ContextFrame> frames = new ArrayList<>(contextStack);
        Collections.reverse(frames);
        for (ContextFrame frame : frames) {
            inherited.putAll(frame.getInheritedValues());
        }
        return inherited;
    }

    /**
     * Get all inherited options with their ProcessedOption metadata.
     * Returns a map of field names to ProcessedOptions for injection.
     *
     * @return map of field names to ProcessedOptions
     */
    public Map<String, ProcessedOption> getAllInheritedOptions() {
        Map<String, ProcessedOption> inherited = new HashMap<>();
        // Process from root to immediate parent to get correct override order
        List<ContextFrame> frames = new ArrayList<>(contextStack);
        Collections.reverse(frames);
        for (ContextFrame frame : frames) {
            inherited.putAll(frame.getInheritedOptions());
        }
        return inherited;
    }

    /**
     * Format context values for display.
     *
     * @return formatted string showing all context values
     */
    public String formatContextValues() {
        if (contextStack.isEmpty()) {
            return "Not in sub-command mode.";
        }

        StringBuilder sb = new StringBuilder();
        List<ContextFrame> frames = new ArrayList<>(contextStack);
        Collections.reverse(frames);

        for (ContextFrame frame : frames) {
            sb.append("Context: ").append(frame.getCommandName()).append("\n");
            Map<String, Object> values = frame.getAllValues();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                // Skip internal keys
                if (!entry.getKey().startsWith("_")) {
                    sb.append("  ").append(entry.getKey()).append(": ")
                            .append(entry.getValue()).append("\n");
                }
            }
        }
        return sb.toString();
    }

    // ========== Context Frame Inner Class ==========

    /**
     * Represents a single frame in the context stack.
     * Contains the parser, command instance, and provides value access.
     */
    public static class ContextFrame {
        private final CommandLineParser<?> parser;
        private final Command<?> command;
        private final Map<String, Object> cachedValues;
        private final Map<String, Object> inheritedValues;
        private final Map<String, ProcessedOption> inheritedOptions;

        public ContextFrame(CommandLineParser<?> parser, Command<?> command) {
            this.parser = parser;
            this.command = command;
            this.cachedValues = new HashMap<>();
            this.inheritedValues = new HashMap<>();
            this.inheritedOptions = new HashMap<>();
            cacheFieldValues();
        }

        private void cacheFieldValues() {
            // Cache all option and argument values for quick access
            ProcessedCommand<?, ?> pc = parser.getProcessedCommand();

            for (ProcessedOption opt : pc.getOptions()) {
                Object value = getFieldValueByReflection(opt.getFieldName());
                if (value != null) {
                    cachedValues.put(opt.getFieldName(), value);
                    // Also cache by option name for convenience
                    if (opt.name() != null && !opt.name().isEmpty()) {
                        cachedValues.put(opt.name(), value);
                    }
                    // Track inherited options separately
                    if (opt.isInherited()) {
                        inheritedValues.put(opt.getFieldName(), value);
                        inheritedOptions.put(opt.getFieldName(), opt);
                        if (opt.name() != null && !opt.name().isEmpty()) {
                            inheritedValues.put(opt.name(), value);
                        }
                    }
                }
            }

            // Cache argument
            if (pc.getArgument() != null) {
                ProcessedOption arg = pc.getArgument();
                Object value = getFieldValueByReflection(arg.getFieldName());
                if (value != null) {
                    cachedValues.put(arg.getFieldName(), value);
                    cachedValues.put("_argument", value);
                    // Track inherited argument
                    if (arg.isInherited()) {
                        inheritedValues.put(arg.getFieldName(), value);
                        inheritedOptions.put(arg.getFieldName(), arg);
                    }
                }
            }

            // Cache arguments (list)
            if (pc.getArguments() != null) {
                Object value = getFieldValueByReflection(pc.getArguments().getFieldName());
                if (value != null) {
                    cachedValues.put(pc.getArguments().getFieldName(), value);
                    cachedValues.put("_arguments", value);
                }
            }
        }

        private Object getFieldValueByReflection(String fieldName) {
            try {
                Field field = findField(command.getClass(), fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    return field.get(command);
                }
            } catch (IllegalAccessException e) {
                // Ignore, return null
            }
            return null;
        }

        private Field findField(Class<?> clazz, String fieldName) {
            while (clazz != null) {
                try {
                    return clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            return null;
        }

        /**
         * Get the command name.
         *
         * @return the command name
         */
        public String getCommandName() {
            return parser.getProcessedCommand().name();
        }

        /**
         * Get the command instance with cached values restored.
         * This is important because parsing subsequent commands may reset the command's fields.
         *
         * @return the command with restored values
         */
        public Command<?> getCommand() {
            restoreCachedValues();
            return command;
        }

        /**
         * Restore cached values back to the command instance.
         * This is needed because parsing may reset the command's fields.
         */
        private void restoreCachedValues() {
            for (Map.Entry<String, Object> entry : cachedValues.entrySet()) {
                try {
                    Field field = findField(command.getClass(), entry.getKey());
                    if (field != null) {
                        field.setAccessible(true);
                        field.set(command, entry.getValue());
                    }
                } catch (IllegalAccessException e) {
                    // Ignore, continue with other fields
                }
            }
        }

        /**
         * Get the parser.
         *
         * @return the command line parser
         */
        public CommandLineParser<?> getParser() {
            return parser;
        }

        /**
         * Get a field value by name.
         *
         * @param fieldName the field name
         * @return the value, or null if not found
         */
        public Object getFieldValue(String fieldName) {
            return cachedValues.get(fieldName);
        }

        /**
         * Get the primary value for display in prompt.
         * Looks for arguments first, then common option names like "name", "projectName".
         *
         * @return the primary value as string, or null if none
         */
        public String getPrimaryArgumentValue() {
            // First check for @Argument
            Object arg = cachedValues.get("_argument");
            if (arg != null) {
                return arg.toString();
            }
            // Check for @Arguments
            Object args = cachedValues.get("_arguments");
            if (args instanceof List && !((List<?>) args).isEmpty()) {
                return ((List<?>) args).get(0).toString();
            }
            // Look for common option names that typically identify the context
            String[] commonNames = { "name", "projectName", "moduleName", "id", "target" };
            for (String name : commonNames) {
                Object value = cachedValues.get(name);
                if (value != null) {
                    return value.toString();
                }
            }
            return null;
        }

        /**
         * Get all cached values.
         *
         * @return unmodifiable map of all values
         */
        public Map<String, Object> getAllValues() {
            return Collections.unmodifiableMap(cachedValues);
        }

        /**
         * Get an inherited value by field or option name.
         *
         * @param fieldName the field or option name
         * @return the inherited value, or null if not found or not inherited
         */
        public Object getInheritedValue(String fieldName) {
            return inheritedValues.get(fieldName);
        }

        /**
         * Get all inherited values from this frame.
         *
         * @return unmodifiable map of inherited values
         */
        public Map<String, Object> getInheritedValues() {
            return Collections.unmodifiableMap(inheritedValues);
        }

        /**
         * Get all inherited options from this frame.
         *
         * @return unmodifiable map of inherited ProcessedOptions
         */
        public Map<String, ProcessedOption> getInheritedOptions() {
            return Collections.unmodifiableMap(inheritedOptions);
        }

        /**
         * Format entry message showing all values.
         *
         * @return formatted string for display
         */
        public String formatEntryMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append("Entering ").append(getCommandName()).append(" mode:");

            boolean hasValues = false;
            for (Map.Entry<String, Object> entry : cachedValues.entrySet()) {
                // Skip internal keys
                if (!entry.getKey().startsWith("_")) {
                    sb.append("\n  ").append(entry.getKey()).append(": ").append(entry.getValue());
                    hasValues = true;
                }
            }

            if (!hasValues) {
                sb.append("\n  (no options set)");
            }

            sb.append("\nType 'exit' to return.");
            return sb.toString();
        }
    }
}
