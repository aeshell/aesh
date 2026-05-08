# Implementation Plan: Sub-Command Mode for Group Commands

## Issue Reference
GitHub Issue: https://github.com/aeshell/aesh/issues/328

## Overview

This proposal outlines how to implement a "sub-command mode" feature that allows users to enter an interactive context for group commands. When a user types a group command name (e.g., `module`), the shell enters a mode where:

1. The prompt changes to reflect the current context (e.g., `module>`)
2. Only subcommands of that group are available
3. Subcommands can be typed without the group prefix
4. Nested group commands create nested contexts (e.g., `module:project>`)
5. Users can exit the mode to return to the parent context

## User Experience

### Example Session

```bash
aesh> module                          # Enter group command mode
Entering module mode. Type 'exit' to return.
module> tag add --name release-1.0    # No need for "module tag add ..."
Tag 'release-1.0' added.
module> project                       # Enter nested group
Entering project mode.
module:project> add --name myproject  # Nested context
Project 'myproject' added.
module:project> exit                  # Exit nested context
module> exit                          # Exit to root
aesh>                                 # Back to normal prompt
```

### Key Behaviors

1. **Entering Sub-Command Mode**: Typing just the group command name (without a subcommand) enters the mode
2. **Command Execution**: Commands are automatically prefixed with the current context path
3. **Tab Completion**: Only shows subcommands of the current context
4. **Exit Command**: A built-in `exit` or `..` command returns to the parent context
5. **Nested Contexts**: Group commands within group commands create deeper nesting
6. **Context Display**: Prompt clearly shows the current context path
7. **Parent Context Sharing**: Parent command options/arguments are available to subcommands

---

## Parent Command Context Sharing

A key enhancement is the ability to pass parent command state (options and arguments) to subcommands. This works for **both** direct invocation and sub-command mode, enabling powerful patterns where a group command establishes context that all subcommands can access.

### When Does Interactive Sub-Command Mode Activate?

**Important:** Interactive sub-command mode is **only** activated when a group command is invoked **without** a subcommand. If a subcommand is specified, the command executes directly (no interactive mode).

| Command | Behavior |
|---------|----------|
| `module --verbose my-module-name tag add v1.0` | Direct execution - runs `tag add`, then returns to prompt |
| `module --verbose my-module-name` | Enters interactive sub-command mode |
| `module` | Enters interactive sub-command mode (no options) |

### Example: Direct Invocation (No Interactive Mode)

When a subcommand is specified, execution is direct - no interactive mode:

```bash
# Direct invocation - executes tag add and returns to normal prompt
aesh> module --verbose my-module-name tag add v1.0
Added tag 'v1.0' to module 'my-module-name' (verbose mode).
aesh>    # Back to normal prompt immediately
```

The `tag` command still has access to the parent's `--verbose` flag and `my-module-name` argument.

### Example: Interactive Sub-Command Mode

Interactive mode only activates when **no subcommand** is specified:

```bash
aesh> module --verbose my-module-name     # No subcommand → enters interactive mode
Entering module mode:
  moduleName: my-module-name
  verbose: true
Type 'exit' to return.
module[my-module-name]> tag add v1.0      # Subcommand can access parent's values
Added tag 'v1.0' to module 'my-module-name' (verbose mode).
module[my-module-name]> info              # Execute multiple subcommands
Module: my-module-name
Verbose: true
module[my-module-name]> exit              # Explicitly exit
aesh>
```

### Context Display Options

When entering sub-command mode, the system displays:

1. **Entry message** with all option/argument values set on the group command
2. **Prompt** showing the command name and primary argument (e.g., `module[my-module-name]>`)
3. **Built-in `context` command** (optional) to redisplay current context values

Example with multiple options:

```bash
aesh> database --host=prod-db.example.com --port=5432 mydb
Entering database mode:
  name: mydb
  host: prod-db.example.com
  port: 5432
Type 'exit' to return.
database[mydb]> query "SELECT * FROM users"
...
database[mydb]> context                   # Show current context again
Current context: database
  name: mydb
  host: prod-db.example.com
  port: 5432
database[mydb]>
```

The display format is configurable via settings:

| Setting | Default | Description |
|---------|---------|-------------|
| `showContextOnEntry` | `true` | Display option/argument values when entering |
| `showArgumentInPrompt` | `true` | Include primary argument in prompt |
| `contextCommand` | `"context"` | Command name to redisplay context (null to disable) |

### Use Cases

1. **Resource Selection**: `database mydb` enters a mode where all subcommands operate on `mydb`
2. **Configuration Inheritance**: `--verbose` on the group command enables verbose mode for all subcommands
3. **Authentication Context**: `connect user@host` establishes connection context for subsequent commands
4. **Namespace Scoping**: `namespace prod` scopes all Kubernetes-like commands to the `prod` namespace

### Access Patterns

Subcommands can access parent context in two ways:

#### Option 1: Via CommandInvocation

```java
@CommandDefinition(name = "tag", description = "Manage tags")
public class TagCommand implements Command<CommandInvocation> {

    @Argument(description = "Tag name")
    private String tagName;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        // Access parent command's parsed values
        CommandContext ctx = invocation.getCommandContext();

        // Get parent's argument value
        String moduleName = ctx.getParentValue("moduleName", String.class);

        // Get parent's option value
        boolean verbose = ctx.getParentValue("verbose", Boolean.class, false);

        invocation.println("Adding tag '" + tagName + "' to module '" + moduleName + "'");
        return CommandResult.SUCCESS;
    }
}
```

#### Option 2: Via @ParentCommand Annotation (Picocli-style)

```java
@CommandDefinition(name = "tag", description = "Manage tags")
public class TagCommand implements Command<CommandInvocation> {

    @ParentCommand
    private ModuleCommand parent;  // Automatically injected

    @Argument(description = "Tag name")
    private String tagName;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        // Direct access to parent command instance
        String moduleName = parent.getModuleName();
        boolean verbose = parent.isVerbose();

        invocation.println("Adding tag '" + tagName + "' to module '" + moduleName + "'");
        return CommandResult.SUCCESS;
    }
}
```

#### Option 3: Via Inherited Options

Options marked with `inherited = true` on the parent are automatically available on subcommands:

```java
@GroupCommandDefinition(name = "module", groupCommands = {TagCommand.class})
public class ModuleCommand implements Command<CommandInvocation> {

    @Argument(description = "Module name", inherited = true)
    private String moduleName;

    @Option(name = "verbose", hasValue = false, inherited = true)
    private boolean verbose;
}

@CommandDefinition(name = "tag", description = "Manage tags")
public class TagCommand implements Command<CommandInvocation> {

    // These are automatically inherited from parent
    // No need to declare moduleName or verbose here

    @Argument(description = "Tag name")
    private String tagName;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        // Access inherited values directly via CommandInvocation
        String moduleName = invocation.getInheritedValue("moduleName");
        // ...
    }
}
```

---

## Architecture

### New Components

#### 1. CommandContext Class

A new class to track the current command context state, including parent command instances and their parsed values.

```java
package org.aesh.command.impl.context;

import org.aesh.command.Command;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;

import java.util.*;

public class CommandContext {
    private final Deque<ContextFrame> contextStack;
    private final String originalPrompt;

    public CommandContext(String originalPrompt) {
        this.contextStack = new ArrayDeque<>();
        this.originalPrompt = originalPrompt;
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
     */
    public ContextFrame pop() {
        return contextStack.pop();
    }

    /**
     * Get the current context frame.
     */
    public ContextFrame current() {
        return contextStack.peek();
    }

    /**
     * Check if we're in sub-command mode.
     */
    public boolean isInSubCommandMode() {
        return !contextStack.isEmpty();
    }

    /**
     * Get the context path as a string (e.g., "module:project").
     */
    public String getContextPath() {
        List<String> names = new ArrayList<>();
        for (ContextFrame frame : contextStack) {
            names.add(0, frame.getCommandName());
        }
        return String.join(":", names);
    }

    /**
     * Build a prompt showing the current context.
     * Examples: "module> ", "module[my-module]> ", "module:project> "
     */
    public String buildPrompt() {
        if (contextStack.isEmpty()) {
            return originalPrompt;
        }

        StringBuilder prompt = new StringBuilder();
        List<ContextFrame> frames = new ArrayList<>(contextStack);
        Collections.reverse(frames);

        for (int i = 0; i < frames.size(); i++) {
            if (i > 0) prompt.append(":");
            ContextFrame frame = frames.get(i);
            prompt.append(frame.getCommandName());

            // Show primary argument value in prompt if present
            String argValue = frame.getPrimaryArgumentValue();
            if (argValue != null && !argValue.isEmpty()) {
                prompt.append("[").append(argValue).append("]");
            }
        }
        prompt.append("> ");
        return prompt.toString();
    }

    public String getOriginalPrompt() {
        return originalPrompt;
    }

    // ========== Parent Value Access Methods ==========

    /**
     * Get a value from any parent command in the context stack.
     * Searches from immediate parent up to root.
     *
     * @param fieldName The field name to look for
     * @param type The expected type
     * @return The value, or null if not found
     */
    public <T> T getParentValue(String fieldName, Class<T> type) {
        return getParentValue(fieldName, type, null);
    }

    /**
     * Get a value from any parent command with a default.
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
     */
    public Command<?> getParentCommand() {
        ContextFrame frame = contextStack.peek();
        return frame != null ? frame.getCommand() : null;
    }

    /**
     * Get all parent commands from immediate to root.
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

    // ========== Context Frame Inner Class ==========

    /**
     * Represents a single frame in the context stack.
     * Contains the parser, command instance, and provides value access.
     */
    public static class ContextFrame {
        private final CommandLineParser<?> parser;
        private final Command<?> command;
        private final Map<String, Object> cachedValues;

        public ContextFrame(CommandLineParser<?> parser, Command<?> command) {
            this.parser = parser;
            this.command = command;
            this.cachedValues = new HashMap<>();
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
                }
            }

            // Cache argument
            if (pc.getArgument() != null) {
                Object value = getFieldValueByReflection(pc.getArgument().getFieldName());
                if (value != null) {
                    cachedValues.put(pc.getArgument().getFieldName(), value);
                    cachedValues.put("_argument", value);
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
                java.lang.reflect.Field field = findField(command.getClass(), fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    return field.get(command);
                }
            } catch (IllegalAccessException e) {
                // Ignore, return null
            }
            return null;
        }

        private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
            while (clazz != null) {
                try {
                    return clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            return null;
        }

        public String getCommandName() {
            return parser.getProcessedCommand().name();
        }

        public Command<?> getCommand() {
            return command;
        }

        public CommandLineParser<?> getParser() {
            return parser;
        }

        public Object getFieldValue(String fieldName) {
            return cachedValues.get(fieldName);
        }

        /**
         * Get the primary argument value for display in prompt.
         */
        public String getPrimaryArgumentValue() {
            Object arg = cachedValues.get("_argument");
            if (arg != null) {
                return arg.toString();
            }
            Object args = cachedValues.get("_arguments");
            if (args instanceof List && !((List<?>) args).isEmpty()) {
                return ((List<?>) args).get(0).toString();
            }
            return null;
        }

        /**
         * Get all cached values (for debugging/inspection).
         */
        public Map<String, Object> getAllValues() {
            return Collections.unmodifiableMap(cachedValues);
        }
    }
}
```

#### 2. SubCommandModeSettings

Configuration options for sub-command mode behavior.

```java
package org.aesh.command.settings;

public interface SubCommandModeSettings {
    // Enable/disable sub-command mode globally
    boolean isSubCommandModeEnabled();

    // Command to exit sub-command mode (default: "exit")
    String getExitCommand();

    // Alternative exit command (default: "..")
    String getAlternativeExitCommand();

    // Prompt separator for nested contexts (default: ":")
    String getContextSeparator();

    // Message shown when entering sub-command mode (supports placeholders)
    String getEnterMessage();

    // Message shown when exiting sub-command mode
    String getExitMessage();

    // Display option/argument values when entering sub-command mode (default: true)
    boolean showContextOnEntry();

    // Include primary argument value in prompt, e.g., "module[myarg]>" (default: true)
    boolean showArgumentInPrompt();

    // Command name to display current context, null to disable (default: "context")
    String getContextCommand();
}
```

#### 3. @ParentCommand Annotation

A new annotation to mark fields that should receive the parent command instance (similar to picocli).

```java
package org.aesh.command.option;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a field to receive the parent command instance when executing
 * as a subcommand. The field type must match the parent command class.
 *
 * <p>Example usage:</p>
 * <pre>
 * {@literal @}CommandDefinition(name = "sub", description = "Subcommand")
 * public class SubCommand implements Command&lt;CommandInvocation&gt; {
 *
 *     {@literal @}ParentCommand
 *     private ParentGroupCommand parent;
 *
 *     public CommandResult execute(CommandInvocation invocation) {
 *         // Access parent's parsed values
 *         String value = parent.getMyOption();
 *         // ...
 *     }
 * }
 * </pre>
 *
 * <p>This annotation works both in sub-command mode (interactive) and
 * when commands are invoked directly (e.g., "parent sub --option value").</p>
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface ParentCommand {
    // No properties needed - the field type determines which parent to inject
}
```

#### 4. @Inherited Option/Argument Property

Extend @Option and @Argument to support inheritance to subcommands.

```java
// In Option.java - add new property:

/**
 * When true, this option is automatically available to all subcommands.
 * Subcommands can access the value via CommandInvocation.getInheritedValue().
 * Only valid on group commands.
 */
boolean inherited() default false;
```

```java
// In Argument.java - add new property:

/**
 * When true, this argument value is available to all subcommands.
 * Subcommands can access the value via CommandInvocation.getInheritedValue().
 * Only valid on group commands.
 */
boolean inherited() default false;
```

---

## Implementation Steps

### Phase 1: Core Infrastructure

#### Step 1.1: Add CommandContext to ReadlineConsole

**File:** `aesh/src/main/java/org/aesh/readline/ReadlineConsole.java`

Add a `CommandContext` field and integrate it with the readline loop.

```java
public class ReadlineConsole implements Console {
    private final CommandContext commandContext;

    public ReadlineConsole(Settings<?, ?> settings) {
        // ... existing code ...
        this.commandContext = new CommandContext();
    }

    // Add getter for use by other components
    public CommandContext getCommandContext() {
        return commandContext;
    }
}
```

#### Step 1.2: Add SubCommandModeSettings to Settings

**File:** `aesh/src/main/java/org/aesh/command/settings/Settings.java`

Add configuration options for sub-command mode.

```java
public interface Settings<C extends Command, CI extends CommandInvocation> {
    // ... existing methods ...

    boolean subCommandModeEnabled();
    String subCommandModeExitCommand();
    String subCommandModeContextSeparator();
}
```

**File:** `aesh/src/main/java/org/aesh/command/settings/SettingsBuilder.java`

Add builder methods for the new settings.

---

### Phase 2: Command Line Processing

#### Step 2.1: Modify processLine() to Handle Sub-Command Mode

**File:** `aesh/src/main/java/org/aesh/readline/ReadlineConsole.java`

```java
private void processLine(String line, Connection conn) {
    try {
        String effectiveLine = line;

        // If in sub-command mode, check for exit command
        if (commandContext.isInSubCommandMode()) {
            String trimmed = line.trim();
            if (isExitCommand(trimmed)) {
                exitSubCommandMode(conn);
                read(conn, readline);
                return;
            }

            // Prefix the line with the context path
            effectiveLine = commandContext.getContextPath()
                .replace(":", " ") + " " + line;
        }

        Executor<? extends CommandInvocation> executor =
            runtime.buildExecutor(effectiveLine);

        // Check if this is a group command without subcommand
        if (shouldEnterSubCommandMode(executor, line)) {
            enterSubCommandMode(executor, conn);
            read(conn, readline);
            return;
        }

        processManager.execute(executor, conn);
    }
    catch (CommandNotFoundException cnfe) {
        // ... existing error handling ...
    }
}

private boolean isExitCommand(String line) {
    return line.equals(settings.subCommandModeExitCommand()) ||
           line.equals("..");
}

private boolean shouldEnterSubCommandMode(Executor<?> executor, String line) {
    if (!settings.subCommandModeEnabled()) {
        return false;
    }

    CommandLineParser<?> parser = executor.getParser();

    // Must be a group command
    if (!parser.isGroupCommand()) {
        return false;
    }

    // Check if a subcommand was parsed
    // If parsedCommand() returns the group parser itself (not a child),
    // then no subcommand was specified
    CommandLineParser<?> parsed = parser.parsedCommand();

    // Enter sub-command mode only if:
    // 1. It's a group command
    // 2. No subcommand was parsed (parsed command is the group itself)
    return parsed == parser || parsed == null;
}

private void enterSubCommandMode(Executor<?> executor, Connection conn) {
    CommandLineParser<?> groupParser = executor.getParser();
    String currentPrompt = prompt.getPromptAsString();

    commandContext.push(groupParser, currentPrompt);

    // Update prompt
    setPrompt(commandContext.buildPrompt(currentPrompt));

    // Show entry message
    conn.write("Entering " + groupParser.getProcessedCommand().name() +
               " mode. Type 'exit' to return." + Config.getLineSeparator());
}

private void exitSubCommandMode(Connection conn) {
    CommandLineParser<?> exited = commandContext.pop();

    // Restore prompt
    if (commandContext.isInSubCommandMode()) {
        setPrompt(commandContext.buildPrompt(commandContext.getOriginalPrompt()));
    } else {
        setPrompt(commandContext.getOriginalPrompt());
    }

    conn.write("Exited " + exited.getProcessedCommand().name() +
               " mode." + Config.getLineSeparator());
}
```

---

### Phase 3: Completion Support

#### Step 3.1: Modify Completion to Respect Context

**File:** `aesh/src/main/java/org/aesh/readline/ReadlineConsole.java`

The completion handler needs to be context-aware.

```java
private void setupCompletion(Readline readline) {
    readline.setCompleter((line, cursor) -> {
        String effectiveLine = line;
        int effectiveCursor = cursor;

        // If in sub-command mode, prefix with context
        if (commandContext.isInSubCommandMode()) {
            String prefix = commandContext.getContextPath()
                .replace(":", " ") + " ";
            effectiveLine = prefix + line;
            effectiveCursor = cursor + prefix.length();
        }

        // Get completions for effective line
        List<Completion> completions = completeOperation(effectiveLine, effectiveCursor);

        // Adjust offsets back to original line positions
        if (commandContext.isInSubCommandMode()) {
            completions = adjustCompletionOffsets(completions, prefix.length());
        }

        return completions;
    });
}
```

#### Step 3.2: Filter Completions to Current Context

When in sub-command mode, initial completions should only show subcommands of the current group.

**File:** `aesh/src/main/java/org/aesh/command/impl/parser/AeshCommandLineCompletionParser.java`

Modify completion logic to filter based on context when applicable.

---

### Phase 4: Help Integration

#### Step 4.1: Context-Aware Help

When in sub-command mode, the `help` command should show only the subcommands available in the current context.

```java
// In help command implementation
if (commandContext.isInSubCommandMode()) {
    CommandLineParser<?> currentGroup = commandContext.current();
    printGroupHelp(currentGroup);
} else {
    printAllCommands();
}
```

---

### Phase 5: Parent Command Injection

This phase implements the mechanism to share parent command state with subcommands.

#### Step 5.1: Implement @ParentCommand Field Injection

**File:** `aesh/src/main/java/org/aesh/command/impl/populator/AeshCommandPopulator.java`

Modify the populator to detect and inject @ParentCommand fields.

```java
public void populateObject(ProcessedCommand<Command<CI>, CI> processedCommand,
                          InvocationProviders invocationProviders,
                          AeshContext aeshContext,
                          CommandLineParser.Mode mode,
                          CommandContext commandContext)  // New parameter
        throws CommandLineParserException, OptionValidatorException {

    // ... existing option/argument population ...

    // Inject parent command if @ParentCommand annotation is present
    if (commandContext != null && commandContext.isInSubCommandMode()) {
        injectParentCommand(getObject(), commandContext);
    }
}

private void injectParentCommand(Object instance, CommandContext commandContext) {
    for (Field field : getAllFields(instance.getClass())) {
        if (field.isAnnotationPresent(ParentCommand.class)) {
            Class<?> fieldType = field.getType();

            // Find matching parent command in context stack
            Command<?> parent = commandContext.getParentCommand(fieldType);

            if (parent != null) {
                try {
                    field.setAccessible(true);
                    field.set(instance, parent);
                } catch (IllegalAccessException e) {
                    // Log warning but continue
                }
            }
        }
    }
}

private List<Field> getAllFields(Class<?> clazz) {
    List<Field> fields = new ArrayList<>();
    while (clazz != null) {
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        clazz = clazz.getSuperclass();
    }
    return fields;
}
```

#### Step 5.2: Extend CommandInvocation for Context Access

**File:** `aesh/src/main/java/org/aesh/command/invocation/CommandInvocation.java`

Add methods to access the command context.

```java
public interface CommandInvocation {
    // ... existing methods ...

    /**
     * Get the current command context (for sub-command mode).
     * Returns null if not in sub-command mode.
     */
    CommandContext getCommandContext();

    /**
     * Get an inherited value from a parent command.
     *
     * @param name The field or option name
     * @param type The expected type
     * @return The value, or null if not found
     */
    <T> T getParentValue(String name, Class<T> type);

    /**
     * Get an inherited value with a default.
     */
    <T> T getParentValue(String name, Class<T> type, T defaultValue);

    /**
     * Check if currently executing in sub-command mode.
     */
    default boolean isInSubCommandMode() {
        CommandContext ctx = getCommandContext();
        return ctx != null && ctx.isInSubCommandMode();
    }
}
```

#### Step 5.3: Update AeshCommandInvocation Implementation

**File:** `aesh/src/main/java/org/aesh/command/impl/invocation/AeshCommandInvocation.java`

```java
public final class AeshCommandInvocation implements CommandInvocation {
    // ... existing fields ...
    private final CommandContext commandContext;

    public AeshCommandInvocation(Console console, Shell shell,
                                  CommandRuntime<AeshCommandInvocation> runtime,
                                  CommandInvocationConfiguration config,
                                  CommandContainer<AeshCommandInvocation> commandContainer,
                                  CommandContext commandContext) {
        // ... existing initialization ...
        this.commandContext = commandContext;
    }

    @Override
    public CommandContext getCommandContext() {
        return commandContext;
    }

    @Override
    public <T> T getParentValue(String name, Class<T> type) {
        return commandContext != null ? commandContext.getParentValue(name, type) : null;
    }

    @Override
    public <T> T getParentValue(String name, Class<T> type, T defaultValue) {
        return commandContext != null
            ? commandContext.getParentValue(name, type, defaultValue)
            : defaultValue;
    }
}
```

#### Step 5.4: Handle Inherited Options

**File:** `aesh/src/main/java/org/aesh/command/impl/internal/ProcessedOption.java`

Add support for the `inherited` flag.

```java
public final class ProcessedOption {
    // ... existing fields ...
    private boolean inherited = false;

    public boolean isInherited() {
        return inherited;
    }
}
```

When entering sub-command mode with inherited options, the values are automatically made available to subcommands through the CommandContext.

---

### Phase 6: Annotation Support (Optional Enhancement)

#### Step 5.1: Add SubCommandMode Annotation Property

Allow commands to opt-in or opt-out of sub-command mode.

**File:** `aesh/src/main/java/org/aesh/command/GroupCommandDefinition.java`

```java
@Retention(RUNTIME)
@Target(TYPE)
public @interface GroupCommandDefinition {
    // ... existing properties ...

    /**
     * If true, typing just the group command name enters sub-command mode.
     * Default is true when sub-command mode is enabled globally.
     */
    boolean enableSubCommandMode() default true;

    /**
     * Custom prompt suffix for this group's sub-command mode.
     * Default uses the command name.
     */
    String subCommandPrompt() default "";
}
```

---

## API Changes Summary

### New Classes

| Class | Package | Purpose |
|-------|---------|---------|
| `CommandContext` | `org.aesh.command.impl.context` | Tracks current sub-command context state and parent command values |
| `CommandContext.ContextFrame` | `org.aesh.command.impl.context` | Single frame in context stack with cached field values |
| `SubCommandModeSettings` | `org.aesh.command.settings` | Configuration for sub-command mode |

### New Annotations

| Annotation | Package | Purpose |
|------------|---------|---------|
| `@ParentCommand` | `org.aesh.command.option` | Marks a field to receive the parent command instance |

### Modified Annotations

| Annotation | Changes |
|------------|---------|
| `@Option` | Add `inherited` property for parent-to-child value passing |
| `@Argument` | Add `inherited` property for parent-to-child value passing |
| `@GroupCommandDefinition` | Add `enableSubCommandMode` and `subCommandPrompt` properties |

### Modified Classes

| Class | Changes |
|-------|---------|
| `ReadlineConsole` | Add `CommandContext` field, modify `processLine()` and completion handling |
| `Settings` / `SettingsBuilder` | Add sub-command mode configuration options |
| `CommandInvocation` | Add `getCommandContext()`, `getParentValue()`, `isInSubCommandMode()` methods |
| `AeshCommandInvocation` | Implement new `CommandInvocation` methods, store `CommandContext` |
| `AeshCommandPopulator` | Inject `@ParentCommand` fields, handle inherited options |
| `ProcessedOption` | Add `inherited` field and getter |
| `AeshCommandLineCompletionParser` | Context-aware completion filtering |

### New Settings

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `subCommandModeEnabled` | `boolean` | `false` | Enable sub-command mode globally |
| `subCommandModeExitCommand` | `String` | `"exit"` | Command to exit sub-command mode |
| `subCommandModeContextSeparator` | `String` | `":"` | Separator for nested context display |
| `showContextOnEntry` | `boolean` | `true` | Display option/argument values when entering |
| `showArgumentInPrompt` | `boolean` | `true` | Include primary argument in prompt |
| `contextCommand` | `String` | `"context"` | Command to redisplay context (null to disable) |

---

## Implementation Considerations

### 1. Backward Compatibility

- Sub-command mode is **disabled by default** to maintain backward compatibility
- Existing group command behavior (requiring full `group subcommand` syntax) remains unchanged when disabled
- No breaking changes to existing APIs

### 2. Edge Cases

| Scenario | Behavior |
|----------|----------|
| Typing `exit` when `exit` is also a subcommand | Exit command takes precedence; use `./exit` for subcommand |
| Group command has options | Options can still be passed: `module --verbose` enters mode with verbose flag |
| Empty input in sub-command mode | Shows available subcommands (like pressing Tab) |
| Ctrl-C in sub-command mode | Exits current context (configurable) |
| Nested groups 3+ levels deep | Fully supported with stacked contexts |

### 3. Thread Safety

- `CommandContext` should be per-connection/session, not global
- Use appropriate synchronization if needed for concurrent access

### 4. Testing Strategy

1. **Unit Tests**: Test `CommandContext` push/pop/path generation
2. **Integration Tests**: Test full flow from entering to exiting sub-command mode
3. **Completion Tests**: Verify completions are properly filtered
4. **Nested Context Tests**: Test 3+ levels of nesting
5. **Edge Case Tests**: Exit command conflicts, empty input, Ctrl-C handling

---

## Example Usage

### Enabling Sub-Command Mode

```java
Settings settings = SettingsBuilder.builder()
    .subCommandModeEnabled(true)
    .subCommandModeExitCommand("exit")
    .subCommandModeContextSeparator(":")
    .build();

CommandRuntime runtime = AeshCommandRuntimeBuilder.builder()
    .settings(settings)
    .command(ModuleCommand.class)
    .build();
```

### Group Command Definition

```java
@GroupCommandDefinition(
    name = "module",
    description = "Module management commands",
    enableSubCommandMode = true,
    subCommandPrompt = "mod",  // Optional: custom prompt
    groupCommands = {TagCommand.class, ProjectCommand.class}
)
public class ModuleCommand implements Command<CommandInvocation> {
    @Override
    public CommandResult execute(CommandInvocation invocation) {
        // This is called when entering sub-command mode
        // Can perform initialization, show welcome message, etc.
        return CommandResult.SUCCESS;
    }
}

@GroupCommandDefinition(
    name = "project",
    description = "Project subcommands",
    groupCommands = {AddProjectCommand.class, DeleteProjectCommand.class}
)
public class ProjectCommand implements Command<CommandInvocation> {
    // Nested group command - creates module:project> context
}
```

---

## Milestones

### Milestone 1: Basic Sub-Command Mode
- [ ] Implement `CommandContext` class with `ContextFrame`
- [ ] Add settings for sub-command mode
- [ ] Modify `processLine()` to detect and enter sub-command mode
- [ ] Implement exit command handling
- [ ] Basic prompt switching

### Milestone 2: Parent Command Context Sharing
- [ ] Implement `@ParentCommand` annotation
- [ ] Modify `AeshCommandPopulator` to inject parent command instances
- [ ] Add `getCommandContext()` and `getParentValue()` to `CommandInvocation`
- [ ] Cache parent command field values in `ContextFrame`
- [ ] Support typed parent command lookup

### Milestone 3: Inherited Options/Arguments
- [ ] Add `inherited` property to `@Option` annotation
- [ ] Add `inherited` property to `@Argument` annotation
- [ ] Modify `ProcessedOption` to track inherited flag
- [ ] Auto-populate inherited values in subcommands

### Milestone 4: Completion Support
- [ ] Context-aware completion in `ReadlineConsole`
- [ ] Filter completions to current context subcommands
- [ ] Adjust completion offsets for prefixed lines

### Milestone 5: Nested Contexts
- [ ] Support multiple levels of nesting
- [ ] Proper context path building (`module:project:task>`)
- [ ] Stack-based prompt restoration
- [ ] Nested parent value access (grandparent values)

### Milestone 6: Polish & Configuration
- [ ] Add annotation properties for per-command configuration
- [ ] Help command integration
- [ ] Ctrl-C handling in sub-command mode
- [ ] Show argument values in prompt (`module[my-module]>`)
- [ ] Documentation and examples

---

## Open Questions

1. **Should the group command's `execute()` method be called when entering sub-command mode?**
   - Option A: Yes, allows initialization logic
   - Option B: No, only called with explicit subcommand
   - Recommendation: Option A with a flag indicating sub-command mode entry

2. **How should command history work in sub-command mode?**
   - Option A: Store full commands (`module tag add`)
   - Option B: Store context-relative commands (`tag add`)
   - Recommendation: Option A for consistency and replayability

3. **Should there be a visual indicator beyond the prompt?**
   - Consider: colored prompt, status bar, entry message
   - Recommendation: Entry/exit messages + prompt change (configurable)

4. **How should parent context work when invoking commands directly (not in sub-command mode)?**
   - Example: `module --myoption=my-value tag add` - should `tag` have access to `myoption`?
   - **Decision: Yes** - Parent values are always available to subcommands regardless of invocation style
   - This matches picocli behavior and user expectations
   - Implementation: The parser already populates the parent command before parsing the subcommand, so we just need to pass the parent instance to the subcommand's populator

5. **What happens if a subcommand defines the same option/argument as the parent?**
   - Option A: Subcommand value overrides parent
   - Option B: Both available, with explicit access methods
   - Option C: Error at command registration time
   - Recommendation: Option A with access to parent via `getParentValue()`

6. **Should inherited options appear in subcommand help?**
   - Option A: Yes, show all available options including inherited
   - Option B: No, only show subcommand-specific options
   - Option C: Configurable per-option
   - Recommendation: Option A with visual indicator (e.g., "[inherited]")

7. **How should validation work for inherited values?**
   - Parent values are already validated when parent executes
   - Should subcommands be able to add additional validation?
   - Recommendation: Parent validation only; subcommands trust parent values

---

## Conclusion

This implementation plan provides a comprehensive approach to adding sub-command mode to aesh. The design:

- Maintains full backward compatibility
- Provides a clean, intuitive user experience
- Supports arbitrary nesting depth
- Enables parent-to-child context sharing via multiple mechanisms:
  - `@ParentCommand` annotation for direct parent instance access
  - `CommandInvocation.getParentValue()` for value lookup
  - `inherited` option property for automatic value inheritance
- Integrates with existing completion and help systems
- Is highly configurable

The phased approach allows for incremental implementation and testing, with each milestone delivering usable functionality.

### Comparison to Picocli

| Feature | Picocli | Aesh (Proposed) |
|---------|---------|-----------------|
| Sub-command mode | Not built-in | Full support |
| `@ParentCommand` | Yes | Yes (new) |
| Inherited options | `scope = INHERIT` | `inherited = true` (new) |
| Context prompt | N/A | Configurable |
| Nested contexts | N/A | Full support |
| Value access | Direct field access | Field access + `CommandInvocation` API |

This proposal brings aesh to feature parity with picocli for parent-child command relationships while adding unique interactive sub-command mode capabilities.
