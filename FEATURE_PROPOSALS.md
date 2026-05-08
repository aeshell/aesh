# Aesh Feature Proposals: Picocli-Inspired Enhancements

This document contains detailed implementation proposals for features inspired by picocli that would enhance aesh's CLI framework capabilities.

---

## 1. Negatable Options

### Overview
Support for `--no-XXX` style options that negate boolean flags. This is a common CLI convention (e.g., `--no-color`, `--no-cache`).

### API Design

```java
@Option(name = "verbose", negatable = true)
boolean verbose = true;  // --verbose or --no-verbose
```

### Implementation

#### 1.1 Modify `@Option` annotation

**File:** `aesh/src/main/java/org/aesh/command/option/Option.java`

```java
/**
 * When set to true for boolean options, automatically supports --no-{name}
 * to set the value to false. The default prefix is "no-" but can be customized.
 */
boolean negatable() default false;

/**
 * The prefix used for negation. Default is "no-".
 * Only used when negatable=true.
 */
String negationPrefix() default "no-";
```

#### 1.2 Extend `ProcessedOption`

**File:** `aesh/src/main/java/org/aesh/command/impl/internal/ProcessedOption.java`

Add fields:
```java
private boolean negatable;
private String negationPrefix;

public boolean isNegatable() { return negatable; }
public String getNegationPrefix() { return negationPrefix; }
public String getNegatedName() {
    return negatable ? negationPrefix + name : null;
}
```

#### 1.3 Extend `ProcessedOptionBuilder`

**File:** `aesh/src/main/java/org/aesh/command/impl/internal/ProcessedOptionBuilder.java`

```java
public ProcessedOptionBuilder negatable(boolean negatable) {
    this.negatable = negatable;
    return this;
}

public ProcessedOptionBuilder negationPrefix(String prefix) {
    this.negationPrefix = prefix;
    return this;
}
```

#### 1.4 Update `AeshCommandContainerBuilder`

**File:** `aesh/src/main/java/org/aesh/command/impl/container/AeshCommandContainerBuilder.java`

In `processField()` for Option:
```java
.negatable(o.negatable())
.negationPrefix(o.negationPrefix())
```

#### 1.5 Modify `AeshCommandLineParser`

**File:** `aesh/src/main/java/org/aesh/command/impl/parser/AeshCommandLineParser.java`

In option matching logic:
```java
// When looking for option by name, also check negated form
ProcessedOption option = findOption(name);
if (option == null) {
    // Check if this is a negated option
    for (ProcessedOption opt : processedCommand.getOptions()) {
        if (opt.isNegatable() && name.equals(opt.getNegatedName())) {
            option = opt;
            negatedValue = true;
            break;
        }
    }
}
```

#### 1.6 Update Help Generation

Show both forms in help:
```
--verbose, --no-verbose    Enable verbose output (default: true)
```

### Validation
- Only allow `negatable=true` for boolean/Boolean fields
- Throw `CommandLineParserException` at build time if misused

---

## 2. Argument Groups (Mutual Exclusivity & Dependency)

### Overview
Support for grouping options with constraints:
- **Exclusive**: Only one option in the group can be specified
- **Dependent**: If one option is specified, all must be specified
- **Multiplicity**: Control how many times a group can appear

### API Design

#### New Annotation: `@OptionGroup` (rename existing to `@OptionMap`)

```java
@Retention(RUNTIME)
@Target(FIELD)
public @interface ArgGroup {
    /**
     * Name of the group for error messages and help.
     */
    String name() default "";

    /**
     * If true, options in this group are mutually exclusive.
     */
    boolean exclusive() default false;

    /**
     * If true, all options in this group must be specified together.
     */
    boolean dependent() default false;

    /**
     * Multiplicity constraint: "0..1", "1", "0..*", "1..*"
     */
    String multiplicity() default "0..1";

    /**
     * Heading for this group in help output.
     */
    String heading() default "";

    /**
     * Order in help output.
     */
    int order() default -1;
}
```

#### Usage Example

```java
@CommandDefinition(name = "myapp", description = "My Application")
public class MyCommand implements Command {

    // Exclusive group - only one can be specified
    @ArgGroup(exclusive = true, multiplicity = "1",
              heading = "Output Format (choose one)")
    OutputFormat format;

    static class OutputFormat {
        @Option(name = "json", description = "Output as JSON")
        boolean json;

        @Option(name = "xml", description = "Output as XML")
        boolean xml;

        @Option(name = "csv", description = "Output as CSV")
        boolean csv;
    }

    // Dependent group - all or none
    @ArgGroup(dependent = true, heading = "Authentication")
    AuthOptions auth;

    static class AuthOptions {
        @Option(name = "user", required = true)
        String username;

        @Option(name = "password", required = true)
        String password;
    }

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        // ...
    }
}
```

### Implementation

#### 2.1 Create `ArgGroup` Annotation

**File:** `aesh/src/main/java/org/aesh/command/option/ArgGroup.java`

```java
package org.aesh.command.option;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(FIELD)
public @interface ArgGroup {
    String name() default "";
    boolean exclusive() default false;
    boolean dependent() default false;
    String multiplicity() default "0..1";
    String heading() default "";
    int order() default -1;
}
```

#### 2.2 Create `ProcessedArgGroup`

**File:** `aesh/src/main/java/org/aesh/command/impl/internal/ProcessedArgGroup.java`

```java
package org.aesh.command.impl.internal;

import java.util.List;
import java.util.ArrayList;

public class ProcessedArgGroup {
    private final String name;
    private final boolean exclusive;
    private final boolean dependent;
    private final String multiplicity;
    private final String heading;
    private final int order;
    private final List<ProcessedOption> options;
    private final Class<?> groupClass;
    private final String fieldName;

    // Track how many times this group has been specified
    private int specifiedCount = 0;

    public ProcessedArgGroup(String name, boolean exclusive, boolean dependent,
                             String multiplicity, String heading, int order,
                             Class<?> groupClass, String fieldName) {
        this.name = name;
        this.exclusive = exclusive;
        this.dependent = dependent;
        this.multiplicity = multiplicity;
        this.heading = heading;
        this.order = order;
        this.groupClass = groupClass;
        this.fieldName = fieldName;
        this.options = new ArrayList<>();
    }

    public void addOption(ProcessedOption option) {
        options.add(option);
        option.setArgGroup(this);
    }

    public List<ProcessedOption> getOptions() {
        return options;
    }

    public void validate() throws OptionValidatorException {
        List<ProcessedOption> specified = getSpecifiedOptions();

        if (exclusive && specified.size() > 1) {
            throw new OptionValidatorException(
                "Options " + getOptionNames(specified) + " are mutually exclusive");
        }

        if (dependent && specified.size() > 0 && specified.size() < options.size()) {
            List<ProcessedOption> missing = getMissingOptions(specified);
            throw new OptionValidatorException(
                "When using " + getOptionNames(specified) +
                ", you must also specify " + getOptionNames(missing));
        }

        validateMultiplicity(specified);
    }

    private List<ProcessedOption> getSpecifiedOptions() {
        List<ProcessedOption> specified = new ArrayList<>();
        for (ProcessedOption opt : options) {
            if (opt.getValue() != null || opt.getValues().size() > 0) {
                specified.add(opt);
            }
        }
        return specified;
    }

    // ... additional helper methods
}
```

#### 2.3 Extend `ProcessedCommand`

Add support for argument groups:
```java
private List<ProcessedArgGroup> argGroups = new ArrayList<>();

public void addArgGroup(ProcessedArgGroup group) {
    argGroups.add(group);
}

public List<ProcessedArgGroup> getArgGroups() {
    return argGroups;
}
```

#### 2.4 Update `AeshCommandContainerBuilder.processField()`

```java
ArgGroup ag;
if ((ag = field.getAnnotation(ArgGroup.class)) != null) {
    Class<?> groupClass = field.getType();

    ProcessedArgGroup argGroup = new ProcessedArgGroup(
        ag.name().isEmpty() ? field.getName() : ag.name(),
        ag.exclusive(),
        ag.dependent(),
        ag.multiplicity(),
        ag.heading(),
        ag.order(),
        groupClass,
        field.getName()
    );

    // Process all fields in the group class
    for (Field groupField : groupClass.getDeclaredFields()) {
        ProcessedOption option = processFieldAsOption(groupField);
        if (option != null) {
            argGroup.addOption(option);
            processedCommand.addOption(option);
        }
    }

    processedCommand.addArgGroup(argGroup);
}
```

#### 2.5 Update Validation

In `AeshCommandLineParser.parse()` or `populate()`:
```java
// After all options are parsed, validate argument groups
for (ProcessedArgGroup group : processedCommand.getArgGroups()) {
    group.validate();
}
```

#### 2.6 Update Help Generation

Group options under headings in help output:
```
Output Format (choose one):
  --json          Output as JSON
  --xml           Output as XML
  --csv           Output as CSV

Authentication:
  --user          Username (required with --password)
  --password      Password (required with --user)
```

---

## 3. Mixins (Reusable Option Groups)

### Overview
Allow defining reusable sets of options that can be included in multiple commands without code duplication.

### API Design

```java
// Define a reusable mixin
public class LoggingMixin {
    @Option(name = "verbose", shortName = 'v', description = "Verbose output")
    boolean verbose;

    @Option(name = "debug", shortName = 'd', description = "Debug output")
    boolean debug;

    @Option(name = "quiet", shortName = 'q', description = "Quiet mode")
    boolean quiet;
}

// Use the mixin in a command
@CommandDefinition(name = "mycommand", description = "My Command")
public class MyCommand implements Command {

    @Mixin
    LoggingMixin logging;

    @Option(name = "output", shortName = 'o')
    String output;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        if (logging.verbose) {
            // ...
        }
    }
}
```

### Implementation

#### 3.1 Create `@Mixin` Annotation

**File:** `aesh/src/main/java/org/aesh/command/option/Mixin.java`

```java
package org.aesh.command.option;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a field as a mixin, meaning its options should be
 * incorporated into the parent command.
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Mixin {
    /**
     * Optional name for the mixin, used in error messages.
     */
    String name() default "";
}
```

#### 3.2 Update `AeshCommandContainerBuilder.processField()`

```java
Mixin mixin;
if ((mixin = field.getAnnotation(Mixin.class)) != null) {
    Class<?> mixinClass = field.getType();
    String mixinName = mixin.name().isEmpty() ? field.getName() : mixin.name();

    // Process all fields in the mixin class as options of this command
    processMixinClass(processedCommand, mixinClass, field.getName());
}

private static void processMixinClass(ProcessedCommand processedCommand,
                                       Class<?> mixinClass,
                                       String mixinFieldName)
        throws CommandLineParserException {
    for (Field field : mixinClass.getDeclaredFields()) {
        ProcessedOption option = processFieldAsOption(field);
        if (option != null) {
            // Mark the option as belonging to a mixin for proper injection
            option.setMixinFieldName(mixinFieldName);
            option.setMixinFieldPath(mixinFieldName + "." + field.getName());
            processedCommand.addOption(option);
        }
    }

    // Process superclasses of mixin
    if (mixinClass.getSuperclass() != null &&
        mixinClass.getSuperclass() != Object.class) {
        processMixinClass(processedCommand, mixinClass.getSuperclass(), mixinFieldName);
    }
}
```

#### 3.3 Extend `ProcessedOption`

```java
private String mixinFieldName;
private String mixinFieldPath;

public void setMixinFieldName(String name) { this.mixinFieldName = name; }
public String getMixinFieldName() { return mixinFieldName; }
public boolean isMixinOption() { return mixinFieldName != null; }
public void setMixinFieldPath(String path) { this.mixinFieldPath = path; }
public String getMixinFieldPath() { return mixinFieldPath; }
```

#### 3.4 Update `AeshCommandPopulator`

**File:** `aesh/src/main/java/org/aesh/command/impl/populator/AeshCommandPopulator.java`

When injecting values, handle mixin fields:
```java
if (option.isMixinOption()) {
    // Get or create the mixin object
    Field mixinField = findField(command.getClass(), option.getMixinFieldName());
    mixinField.setAccessible(true);
    Object mixinInstance = mixinField.get(command);
    if (mixinInstance == null) {
        mixinInstance = mixinField.getType().getDeclaredConstructor().newInstance();
        mixinField.set(command, mixinInstance);
    }

    // Inject value into mixin field
    Field targetField = findField(mixinInstance.getClass(), option.getFieldName());
    targetField.setAccessible(true);
    targetField.set(mixinInstance, convertedValue);
} else {
    // Normal field injection
    field.set(command, convertedValue);
}
```

---

## 4. Default Value Providers

### Overview
Allow loading default values from external sources (config files, environment variables, system properties) via a pluggable provider mechanism.

### API Design

```java
// Provider interface
public interface DefaultValueProvider {
    /**
     * Get the default value for an option.
     * @param optionName the option name (long form)
     * @param commandName the command name
     * @return the default value, or null if not provided
     */
    String defaultValue(String optionName, String commandName);

    /**
     * Get default values for a multi-value option.
     */
    default List<String> defaultValues(String optionName, String commandName) {
        String value = defaultValue(optionName, commandName);
        return value != null ? Collections.singletonList(value) : Collections.emptyList();
    }
}

// Usage in command definition
@CommandDefinition(name = "myapp",
                   description = "My Application",
                   defaultValueProvider = PropertiesDefaultProvider.class)
public class MyCommand implements Command {
    // Options will get defaults from the provider
}

// Or per-option
@Option(name = "output", defaultValueProvider = EnvDefaultProvider.class)
String output;
```

### Built-in Providers

#### 4.1 `PropertiesDefaultProvider`

Loads defaults from a properties file:
```java
public class PropertiesDefaultProvider implements DefaultValueProvider {
    private Properties props;

    public PropertiesDefaultProvider() {
        props = new Properties();
        try {
            // Look for .myapp.properties in user home, then current dir
            Path userHome = Paths.get(System.getProperty("user.home"), ".myapp.properties");
            Path currentDir = Paths.get(".myapp.properties");

            if (Files.exists(userHome)) {
                props.load(Files.newInputStream(userHome));
            } else if (Files.exists(currentDir)) {
                props.load(Files.newInputStream(currentDir));
            }
        } catch (IOException e) {
            // Ignore, no defaults available
        }
    }

    @Override
    public String defaultValue(String optionName, String commandName) {
        // Try command-specific key first, then global
        String value = props.getProperty(commandName + "." + optionName);
        if (value == null) {
            value = props.getProperty(optionName);
        }
        return value;
    }
}
```

#### 4.2 `EnvironmentDefaultProvider`

Loads defaults from environment variables:
```java
public class EnvironmentDefaultProvider implements DefaultValueProvider {
    private final String prefix;

    public EnvironmentDefaultProvider() {
        this("");
    }

    public EnvironmentDefaultProvider(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String defaultValue(String optionName, String commandName) {
        // Convert option name to ENV_VAR_STYLE
        String envName = (prefix + optionName)
            .toUpperCase()
            .replace('-', '_')
            .replace('.', '_');
        return System.getenv(envName);
    }
}
```

#### 4.3 `SystemPropertyDefaultProvider`

```java
public class SystemPropertyDefaultProvider implements DefaultValueProvider {
    private final String prefix;

    public SystemPropertyDefaultProvider() {
        this("");
    }

    public SystemPropertyDefaultProvider(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String defaultValue(String optionName, String commandName) {
        String value = System.getProperty(prefix + commandName + "." + optionName);
        if (value == null) {
            value = System.getProperty(prefix + optionName);
        }
        return value;
    }
}
```

### Implementation

#### 4.4 Modify `@CommandDefinition`

```java
/**
 * Default value provider for all options in this command.
 */
Class<? extends DefaultValueProvider> defaultValueProvider() default NullDefaultValueProvider.class;
```

#### 4.5 Modify `@Option`

```java
/**
 * Override the command-level default value provider for this option.
 */
Class<? extends DefaultValueProvider> defaultValueProvider() default NullDefaultValueProvider.class;
```

#### 4.6 Update `ProcessedCommand`

```java
private DefaultValueProvider defaultValueProvider;

public void setDefaultValueProvider(DefaultValueProvider provider) {
    this.defaultValueProvider = provider;
}

public DefaultValueProvider getDefaultValueProvider() {
    return defaultValueProvider;
}
```

#### 4.7 Update `AeshCommandContainerBuilder`

```java
// In doGenerateCommandLineParser, after creating processedCommand:
if (command.defaultValueProvider() != NullDefaultValueProvider.class) {
    DefaultValueProvider provider = ReflectionUtil.newInstance(command.defaultValueProvider());
    processedCommand.setDefaultValueProvider(provider);
}
```

#### 4.8 Update Default Value Resolution

In `ProcessedOption` or during population:
```java
public List<String> getEffectiveDefaultValues() {
    // First check annotation defaults
    if (!defaultValues.isEmpty()) {
        return defaultValues;
    }

    // Then check option-level provider
    if (optionDefaultValueProvider != null) {
        List<String> values = optionDefaultValueProvider.defaultValues(name, commandName);
        if (!values.isEmpty()) {
            return values;
        }
    }

    // Finally check command-level provider
    if (commandDefaultValueProvider != null) {
        return commandDefaultValueProvider.defaultValues(name, commandName);
    }

    return Collections.emptyList();
}
```

---

## 5. Inherited/Scoped Options

### Overview
Allow options to automatically propagate to all subcommands, reducing boilerplate.

### API Design

```java
@Option(name = "verbose", scope = OptionScope.INHERIT)
boolean verbose;  // Available in this command AND all subcommands
```

### Implementation

#### 5.1 Create `OptionScope` Enum

**File:** `aesh/src/main/java/org/aesh/command/option/OptionScope.java`

```java
package org.aesh.command.option;

public enum OptionScope {
    /**
     * Option is only available in the declaring command (default).
     */
    LOCAL,

    /**
     * Option is available in the declaring command and all subcommands.
     */
    INHERIT
}
```

#### 5.2 Modify `@Option`

```java
/**
 * The scope of this option.
 * INHERIT makes the option available in all subcommands.
 */
OptionScope scope() default OptionScope.LOCAL;
```

#### 5.3 Extend `ProcessedOption`

```java
private OptionScope scope = OptionScope.LOCAL;
private ProcessedOption inheritedFrom;  // Reference to parent option if inherited

public OptionScope getScope() { return scope; }
public void setScope(OptionScope scope) { this.scope = scope; }
public boolean isInherited() { return inheritedFrom != null; }
public ProcessedOption getInheritedFrom() { return inheritedFrom; }
```

#### 5.4 Update Child Command Processing

In `AeshCommandContainerBuilder.doGenerateCommandLineParser()`, when adding child commands:

```java
// Copy inherited options from parent to child
for (ProcessedOption parentOption : processedCommand.getOptions()) {
    if (parentOption.getScope() == OptionScope.INHERIT) {
        ProcessedOption inheritedOption = parentOption.createInheritedCopy();
        childProcessedCommand.addOption(inheritedOption);
    }
}
```

#### 5.5 Value Propagation

When a parent command's inherited option is set, propagate to children:
```java
// In parser or populator
if (option.getScope() == OptionScope.INHERIT) {
    for (CommandLineParser<?> child : parser.getAllChildParsers()) {
        ProcessedOption childOption = child.getProcessedCommand()
            .findOptionByInheritedFrom(option);
        if (childOption != null) {
            childOption.setValue(option.getValue());
        }
    }
}
```

---

## 6. @-File Expansion

### Overview
Support loading command-line arguments from a file, prefixed with `@`.

### API Design

```bash
# Create args file
echo "--verbose" > args.txt
echo "--output result.json" >> args.txt

# Use the file
myapp @args.txt --extra-option
```

```java
@CommandDefinition(name = "myapp",
                   description = "My Application",
                   expandAtFiles = true)  // Enable @-file expansion
public class MyCommand implements Command {
    // ...
}
```

### Implementation

#### 6.1 Modify `@CommandDefinition`

```java
/**
 * If true, arguments starting with @ are treated as file references
 * containing additional arguments (one per line).
 */
boolean expandAtFiles() default false;

/**
 * Character encoding for @-files.
 */
String atFileEncoding() default "UTF-8";
```

#### 6.2 Create `AtFileExpander`

**File:** `aesh/src/main/java/org/aesh/command/impl/parser/AtFileExpander.java`

```java
package org.aesh.command.impl.parser;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

public class AtFileExpander {

    private final Charset charset;
    private final Set<Path> expandedFiles = new HashSet<>();  // Prevent circular refs

    public AtFileExpander(String encoding) {
        this.charset = Charset.forName(encoding);
    }

    public List<String> expand(List<String> args) throws IOException {
        List<String> expanded = new ArrayList<>();

        for (String arg : args) {
            if (arg.startsWith("@") && arg.length() > 1) {
                String filename = arg.substring(1);

                // Handle @@ escape
                if (filename.startsWith("@")) {
                    expanded.add(filename);  // Literal @filename
                    continue;
                }

                Path file = Paths.get(filename).toAbsolutePath();

                if (expandedFiles.contains(file)) {
                    throw new IOException("Circular @-file reference: " + file);
                }
                expandedFiles.add(file);

                List<String> fileArgs = readArgsFromFile(file);
                expanded.addAll(expand(fileArgs));  // Recursive expansion

            } else {
                expanded.add(arg);
            }
        }

        return expanded;
    }

    private List<String> readArgsFromFile(Path file) throws IOException {
        List<String> args = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Handle quoted arguments
                args.addAll(splitLine(line));
            }
        }

        return args;
    }

    private List<String> splitLine(String line) {
        // Parse line respecting quotes
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = 0;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (!inQuote && (c == '"' || c == '\'')) {
                inQuote = true;
                quoteChar = c;
            } else if (inQuote && c == quoteChar) {
                inQuote = false;
            } else if (!inQuote && Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args;
    }
}
```

#### 6.3 Integrate into Parser

In `AeshCommandLineParser.parse()`:
```java
if (processedCommand.isExpandAtFiles()) {
    AtFileExpander expander = new AtFileExpander(processedCommand.getAtFileEncoding());
    try {
        args = expander.expand(args);
    } catch (IOException e) {
        throw new CommandLineParserException("Failed to expand @-file: " + e.getMessage());
    }
}
```

---

## Summary

| Feature | Priority | Complexity | Files to Modify |
|---------|----------|------------|-----------------|
| Negatable Options | High | Low | 5-6 files |
| Argument Groups | High | High | 8-10 files |
| Mixins | High | Medium | 6-8 files |
| Default Value Providers | High | Medium | 6-8 files |
| Inherited Options | Medium | Medium | 5-7 files |
| @-File Expansion | Medium | Low | 3-4 files |

### Recommended Implementation Order

1. **Negatable Options** - Simple, high value, low risk
2. **Default Value Providers** - Medium complexity, enables config file integration
3. **Mixins** - Medium complexity, great for code reuse
4. **@-File Expansion** - Simple addition, useful for complex commands
5. **Inherited Options** - Requires careful design for value propagation
6. **Argument Groups** - Most complex, requires significant parser changes

---

## Testing Strategy

For each feature:
1. Unit tests for new classes
2. Integration tests for end-to-end parsing
3. Help generation tests
4. Completion tests
5. Error message tests
6. Edge case tests (empty values, special characters, etc.)
