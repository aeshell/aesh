package org.aesh.command;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.impl.registry.MutableCommandRegistryImpl;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Mixin;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionGroup;
import org.aesh.command.option.OptionList;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

/**
 * Benchmark comparing aesh startup time (with and without annotation processor)
 * vs picocli (reflection and programmatic) at multiple command count scales.
 * <p>
 * Four paths are measured:
 * <ul>
 * <li><b>Aesh (generated)</b> — simulates the compile-time annotation processor path:
 * builds ProcessedCommand objects via ProcessedCommandBuilder with hardcoded metadata
 * (no runtime reflection on fields/annotations)</li>
 * <li><b>Aesh (reflection)</b> — the runtime reflection path:
 * scans {@code @CommandDefinition}, {@code @Option}, {@code @Argument} annotations
 * on class fields to build ProcessedCommand objects</li>
 * <li><b>Picocli (programmatic)</b> — builds CommandSpec objects via builders with
 * hardcoded metadata (no annotation reflection), analogous to aesh's generated path</li>
 * <li><b>Picocli (reflection)</b> — builds a CommandLine from annotated classes</li>
 * </ul>
 * <p>
 * Run with: {@code mvn -Pbenchmark exec:java -pl benchmark}
 *
 * @author Aesh team
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class StartupBenchmark {

    private static final int WARMUP_ITERATIONS = 1000;
    private static final int MEASURED_ITERATIONS = 3000;
    private static final int[] COMMAND_COUNTS = { 1, 5, 10, 25, 50, 100 };

    /**
     * Reflective access to the private reflection-based command parser.
     * This bypasses MetadataProviderRegistry so we always measure the annotation-scanning path.
     */
    private static final Method REFLECTION_CREATE;
    static {
        try {
            REFLECTION_CREATE = AeshCommandContainerBuilder.class
                    .getDeclaredMethod("doGenerateCommandLineParser", Command.class);
            REFLECTION_CREATE.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---- Flat Aesh command classes (10 distinct commands) ----

    // DefaultValueProvider used by some benchmark commands
    public static class BenchmarkDefaultValueProvider implements DefaultValueProvider {
        @Override
        public String defaultValue(ProcessedOption option) {
            return null; // no dynamic defaults — just tests provider registration overhead
        }
    }

    @CommandDefinition(name = "file-ops", description = "File operations", stopAtFirstPositional = true)
    public static class AeshCmd1 implements Command<CommandInvocation> {
        @Option(shortName = 'o', optionalValue = true, defaultValue = "/tmp/out", description = "Output path")
        private String output;
        @Option(shortName = 'r', hasValue = false, negatable = true, description = "Recursive")
        private boolean recursive;
        @Option(description = "Buffer size", defaultValue = "4096")
        private int bufferSize;
        @Argument(description = "Source path")
        private String source;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "net-fetch", description = "Network fetch", defaultValueProvider = BenchmarkDefaultValueProvider.class)
    public static class AeshCmd2 implements Command<CommandInvocation> {
        @Option(shortName = 'u', description = "URL")
        private String url;
        @Option(shortName = 'v', hasValue = false, negatable = true, description = "Verbose")
        private boolean verbose;
        @Option(description = "Timeout", defaultValue = "30")
        private int timeout;
        @Argument(description = "Destination")
        private String destination;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "db-query", description = "Database query")
    public static class AeshCmd3 implements Command<CommandInvocation> {
        @Option(shortName = 'h', description = "Host")
        private String host;
        @Option(shortName = 'p', optionalValue = true, defaultValue = "5432", description = "Port")
        private String port;
        @Option(shortName = 'q', hasValue = false, negatable = true, description = "Quiet")
        private boolean quiet;
        @Argument(description = "Query string")
        private String query;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "build-proj", description = "Build project", stopAtFirstPositional = true)
    public static class AeshCmd4 implements Command<CommandInvocation> {
        @Option(shortName = 't', description = "Target")
        private String target;
        @Option(hasValue = false, negatable = true, description = "Clean first")
        private boolean clean;
        @Option(description = "Parallelism", defaultValue = "4")
        private int jobs;
        @Argument(description = "Project dir")
        private String projectDir;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "deploy-app", description = "Deploy application", defaultValueProvider = BenchmarkDefaultValueProvider.class)
    public static class AeshCmd5 implements Command<CommandInvocation> {
        @Option(shortName = 'e', description = "Environment")
        private String env;
        @Option(shortName = 'd', hasValue = false, negatable = true, description = "Dry run")
        private boolean dryRun;
        @Option(description = "Replicas", defaultValue = "1")
        private int replicas;
        @Argument(description = "Artifact")
        private String artifact;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "log-search", description = "Search logs")
    public static class AeshCmd6 implements Command<CommandInvocation> {
        @Option(shortName = 'f', description = "Filter pattern")
        private String filter;
        @Option(shortName = 'i', hasValue = false, negatable = true, description = "Case insensitive")
        private boolean ignoreCase;
        @Option(optionalValue = true, defaultValue = "100", description = "Max results")
        private String maxResults;
        @Argument(description = "Log file")
        private String logFile;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "user-mgmt", description = "User management")
    public static class AeshCmd7 implements Command<CommandInvocation> {
        @Option(shortName = 'n', description = "Username")
        private String username;
        @Option(hasValue = false, description = "Admin role")
        private boolean admin;
        @Option(description = "Max sessions", defaultValue = "5")
        private int maxSessions;
        @Argument(description = "Action")
        private String action;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "cache-ctl", description = "Cache control")
    public static class AeshCmd8 implements Command<CommandInvocation> {
        @Option(shortName = 'k', description = "Key prefix")
        private String keyPrefix;
        @Option(hasValue = false, description = "Flush all")
        private boolean flush;
        @Option(description = "TTL seconds", defaultValue = "3600")
        private int ttl;
        @Argument(description = "Region")
        private String region;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "config-edit", description = "Edit configuration")
    public static class AeshCmd9 implements Command<CommandInvocation> {
        @Option(shortName = 's', description = "Section")
        private String section;
        @Option(hasValue = false, description = "Create if missing")
        private boolean create;
        @Option(description = "Indent level", defaultValue = "2")
        private int indent;
        @Argument(description = "Config file")
        private String configFile;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "report-gen", description = "Generate reports")
    public static class AeshCmd10 implements Command<CommandInvocation> {
        @Option(shortName = 'f', description = "Format")
        private String format;
        @Option(hasValue = false, description = "Include charts")
        private boolean charts;
        @Option(description = "Page limit", defaultValue = "50")
        private int pageLimit;
        @Argument(description = "Report name")
        private String reportName;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    // ---- Flat Picocli command classes (10 equivalent commands) ----

    @CommandLine.Command(name = "main", description = "Main app")
    public static class PicocliMain implements Runnable {
        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "file-ops", description = "File operations")
    public static class PicoCmd1 implements Runnable {
        @CommandLine.Option(names = { "-o", "--output" }, description = "Output path")
        private String output;
        @CommandLine.Option(names = { "-r", "--recursive" }, description = "Recursive")
        private boolean recursive;
        @CommandLine.Option(names = "--buffer-size", description = "Buffer size", defaultValue = "4096")
        private int bufferSize;
        @CommandLine.Parameters(index = "0", description = "Source path", arity = "0..1")
        private String source;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "net-fetch", description = "Network fetch")
    public static class PicoCmd2 implements Runnable {
        @CommandLine.Option(names = { "-u", "--url" }, description = "URL")
        private String url;
        @CommandLine.Option(names = { "-v", "--verbose" }, description = "Verbose")
        private boolean verbose;
        @CommandLine.Option(names = "--timeout", description = "Timeout", defaultValue = "30")
        private int timeout;
        @CommandLine.Parameters(index = "0", description = "Destination", arity = "0..1")
        private String destination;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "db-query", description = "Database query")
    public static class PicoCmd3 implements Runnable {
        @CommandLine.Option(names = { "-h", "--host" }, description = "Host")
        private String host;
        @CommandLine.Option(names = { "-p", "--port" }, description = "Port", defaultValue = "5432")
        private int port;
        @CommandLine.Option(names = { "-q", "--quiet" }, description = "Quiet")
        private boolean quiet;
        @CommandLine.Parameters(index = "0", description = "Query string", arity = "0..1")
        private String query;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "build-proj", description = "Build project")
    public static class PicoCmd4 implements Runnable {
        @CommandLine.Option(names = { "-t", "--target" }, description = "Target")
        private String target;
        @CommandLine.Option(names = "--clean", description = "Clean first")
        private boolean clean;
        @CommandLine.Option(names = "--jobs", description = "Parallelism", defaultValue = "4")
        private int jobs;
        @CommandLine.Parameters(index = "0", description = "Project dir", arity = "0..1")
        private String projectDir;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "deploy-app", description = "Deploy application")
    public static class PicoCmd5 implements Runnable {
        @CommandLine.Option(names = { "-e", "--env" }, description = "Environment")
        private String env;
        @CommandLine.Option(names = { "-d", "--dry-run" }, description = "Dry run")
        private boolean dryRun;
        @CommandLine.Option(names = "--replicas", description = "Replicas", defaultValue = "1")
        private int replicas;
        @CommandLine.Parameters(index = "0", description = "Artifact", arity = "0..1")
        private String artifact;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "log-search", description = "Search logs")
    public static class PicoCmd6 implements Runnable {
        @CommandLine.Option(names = { "-f", "--filter" }, description = "Filter pattern")
        private String filter;
        @CommandLine.Option(names = { "-i", "--ignore-case" }, description = "Case insensitive")
        private boolean ignoreCase;
        @CommandLine.Option(names = "--max-results", description = "Max results", defaultValue = "100")
        private int maxResults;
        @CommandLine.Parameters(index = "0", description = "Log file", arity = "0..1")
        private String logFile;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "user-mgmt", description = "User management")
    public static class PicoCmd7 implements Runnable {
        @CommandLine.Option(names = { "-n", "--username" }, description = "Username")
        private String username;
        @CommandLine.Option(names = "--admin", description = "Admin role")
        private boolean admin;
        @CommandLine.Option(names = "--max-sessions", description = "Max sessions", defaultValue = "5")
        private int maxSessions;
        @CommandLine.Parameters(index = "0", description = "Action", arity = "0..1")
        private String action;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "cache-ctl", description = "Cache control")
    public static class PicoCmd8 implements Runnable {
        @CommandLine.Option(names = { "-k", "--key-prefix" }, description = "Key prefix")
        private String keyPrefix;
        @CommandLine.Option(names = "--flush", description = "Flush all")
        private boolean flush;
        @CommandLine.Option(names = "--ttl", description = "TTL seconds", defaultValue = "3600")
        private int ttl;
        @CommandLine.Parameters(index = "0", description = "Region", arity = "0..1")
        private String region;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "config-edit", description = "Edit configuration")
    public static class PicoCmd9 implements Runnable {
        @CommandLine.Option(names = { "-s", "--section" }, description = "Section")
        private String section;
        @CommandLine.Option(names = "--create", description = "Create if missing")
        private boolean create;
        @CommandLine.Option(names = "--indent", description = "Indent level", defaultValue = "2")
        private int indent;
        @CommandLine.Parameters(index = "0", description = "Config file", arity = "0..1")
        private String configFile;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "report-gen", description = "Generate reports")
    public static class PicoCmd10 implements Runnable {
        @CommandLine.Option(names = { "-f", "--format" }, description = "Format")
        private String format;
        @CommandLine.Option(names = "--charts", description = "Include charts")
        private boolean charts;
        @CommandLine.Option(names = "--page-limit", description = "Page limit", defaultValue = "50")
        private int pageLimit;
        @CommandLine.Parameters(index = "0", description = "Report name", arity = "0..1")
        private String reportName;

        @Override
        public void run() {
        }
    }

    // ---- Aesh group command classes (5 groups x 3 = 15 classes) ----

    @GroupCommandDefinition(name = "grp-1", description = "Group 1", groupCommands = { AeshGrpChild1a.class,
            AeshGrpChild1b.class })
    public static class AeshGrpCmd1 implements Command<CommandInvocation> {
        @Option(shortName = 'f', inherited = true, description = "Format")
        private String format;
        @Option(shortName = 'v', hasValue = false, inherited = true, negatable = true, description = "Verbose")
        private boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child-1a", description = "Child 1a")
    public static class AeshGrpChild1a implements Command<CommandInvocation> {
        @Option(shortName = 'i', description = "Input")
        private String input;
        @Option(shortName = 'd', hasValue = false, description = "Debug")
        private boolean debug;
        @Argument(description = "Source")
        private String source;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child-1b", description = "Child 1b")
    public static class AeshGrpChild1b implements Command<CommandInvocation> {
        @Option(shortName = 'n', description = "Name")
        private String name;
        @Option(description = "Count", defaultValue = "10")
        private int count;
        @Argument(description = "Target")
        private String target;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "grp-2", description = "Group 2", groupCommands = { AeshGrpChild2a.class,
            AeshGrpChild2b.class }, defaultValueProvider = BenchmarkDefaultValueProvider.class)
    public static class AeshGrpCmd2 implements Command<CommandInvocation> {
        @Option(shortName = 'e', inherited = true, description = "Environment")
        private String env;
        @Option(shortName = 'q', hasValue = false, inherited = true, description = "Quiet")
        private boolean quiet;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child-2a", description = "Child 2a")
    public static class AeshGrpChild2a implements Command<CommandInvocation> {
        @Option(shortName = 'h', description = "Host")
        private String host;
        @Option(shortName = 'p', description = "Port", defaultValue = "5432")
        private int port;
        @Argument(description = "Address")
        private String addr;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child-2b", description = "Child 2b")
    public static class AeshGrpChild2b implements Command<CommandInvocation> {
        @Option(shortName = 'k', description = "Key")
        private String key;
        @Option(hasValue = false, description = "Force")
        private boolean force;
        @Argument(description = "Path")
        private String path;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "grp-3", description = "Group 3", groupCommands = { AeshGrpChild3a.class,
            AeshGrpChild3b.class }, stopAtFirstPositional = true)
    public static class AeshGrpCmd3 implements Command<CommandInvocation> {
        @Option(shortName = 'l', inherited = true, description = "Level")
        private String level;
        @Option(shortName = 'd', hasValue = false, negatable = true, description = "Dry run")
        private boolean dryRun;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child-3a", description = "Child 3a")
    public static class AeshGrpChild3a implements Command<CommandInvocation> {
        @Option(shortName = 'f', description = "Filter")
        private String filter;
        @Option(description = "Limit", defaultValue = "100")
        private int limit;
        @Argument(description = "Query")
        private String query;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child-3b", description = "Child 3b")
    public static class AeshGrpChild3b implements Command<CommandInvocation> {
        @Option(shortName = 'm', description = "Mode")
        private String mode;
        @Option(description = "Size", defaultValue = "1024")
        private int size;
        @Argument(description = "File")
        private String file;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "grp-4", description = "Group 4", groupCommands = { AeshGrpChild4a.class,
            AeshGrpChild4b.class })
    public static class AeshGrpCmd4 implements Command<CommandInvocation> {
        @Option(shortName = 'r', description = "Region")
        private String region;
        @Option(hasValue = false, description = "Skip validation")
        private boolean skip;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child-4a", description = "Child 4a")
    public static class AeshGrpChild4a implements Command<CommandInvocation> {
        @Option(shortName = 'u', description = "User")
        private String user;
        @Option(hasValue = false, description = "Active")
        private boolean active;
        @Argument(description = "Role")
        private String role;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child-4b", description = "Child 4b")
    public static class AeshGrpChild4b implements Command<CommandInvocation> {
        @Option(shortName = 't', description = "Tag")
        private String tag;
        @Option(description = "Weight", defaultValue = "1")
        private int weight;
        @Argument(description = "Label")
        private String label;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "grp-5", description = "Group 5", groupCommands = { AeshGrpChild5a.class,
            AeshGrpChild5b.class })
    public static class AeshGrpCmd5 implements Command<CommandInvocation> {
        @Option(shortName = 's', description = "Scope")
        private String scope;
        @Option(hasValue = false, description = "Trace")
        private boolean trace;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child-5a", description = "Child 5a")
    public static class AeshGrpChild5a implements Command<CommandInvocation> {
        @Option(shortName = 'p', description = "Pattern")
        private String pattern;
        @Option(hasValue = false, description = "Strict")
        private boolean strict;
        @Argument(description = "Expression")
        private String expr;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child-5b", description = "Child 5b")
    public static class AeshGrpChild5b implements Command<CommandInvocation> {
        @Option(shortName = 'c', description = "Codec")
        private String codec;
        @Option(description = "Depth", defaultValue = "3")
        private int depth;
        @Argument(description = "Data")
        private String data;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    // ---- Picocli group command classes (5 groups x 3 = 15 classes) ----

    @CommandLine.Command(name = "grp-1", description = "Group 1", subcommands = { PicoGrpChild1a.class, PicoGrpChild1b.class })
    public static class PicoGrpCmd1 implements Runnable {
        @CommandLine.Option(names = { "-f", "--format" }, description = "Format")
        private String format;
        @CommandLine.Option(names = { "-v", "--verbose" }, description = "Verbose")
        private boolean verbose;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "child-1a", description = "Child 1a")
    public static class PicoGrpChild1a implements Runnable {
        @CommandLine.Option(names = { "-i", "--input" }, description = "Input")
        private String input;
        @CommandLine.Option(names = { "-d", "--debug" }, description = "Debug")
        private boolean debug;
        @CommandLine.Parameters(index = "0", description = "Source", arity = "0..1")
        private String source;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "child-1b", description = "Child 1b")
    public static class PicoGrpChild1b implements Runnable {
        @CommandLine.Option(names = { "-n", "--name" }, description = "Name")
        private String name;
        @CommandLine.Option(names = "--count", description = "Count", defaultValue = "10")
        private int count;
        @CommandLine.Parameters(index = "0", description = "Target", arity = "0..1")
        private String target;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "grp-2", description = "Group 2", subcommands = { PicoGrpChild2a.class, PicoGrpChild2b.class })
    public static class PicoGrpCmd2 implements Runnable {
        @CommandLine.Option(names = { "-e", "--env" }, description = "Environment")
        private String env;
        @CommandLine.Option(names = { "-q", "--quiet" }, description = "Quiet")
        private boolean quiet;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "child-2a", description = "Child 2a")
    public static class PicoGrpChild2a implements Runnable {
        @CommandLine.Option(names = { "-h", "--host" }, description = "Host")
        private String host;
        @CommandLine.Option(names = { "-p", "--port" }, description = "Port", defaultValue = "5432")
        private int port;
        @CommandLine.Parameters(index = "0", description = "Address", arity = "0..1")
        private String addr;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "child-2b", description = "Child 2b")
    public static class PicoGrpChild2b implements Runnable {
        @CommandLine.Option(names = { "-k", "--key" }, description = "Key")
        private String key;
        @CommandLine.Option(names = "--force", description = "Force")
        private boolean force;
        @CommandLine.Parameters(index = "0", description = "Path", arity = "0..1")
        private String path;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "grp-3", description = "Group 3", subcommands = { PicoGrpChild3a.class, PicoGrpChild3b.class })
    public static class PicoGrpCmd3 implements Runnable {
        @CommandLine.Option(names = { "-l", "--level" }, description = "Level")
        private String level;
        @CommandLine.Option(names = { "-d", "--dry-run" }, description = "Dry run")
        private boolean dryRun;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "child-3a", description = "Child 3a")
    public static class PicoGrpChild3a implements Runnable {
        @CommandLine.Option(names = { "-f", "--filter" }, description = "Filter")
        private String filter;
        @CommandLine.Option(names = "--limit", description = "Limit", defaultValue = "100")
        private int limit;
        @CommandLine.Parameters(index = "0", description = "Query", arity = "0..1")
        private String query;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "child-3b", description = "Child 3b")
    public static class PicoGrpChild3b implements Runnable {
        @CommandLine.Option(names = { "-m", "--mode" }, description = "Mode")
        private String mode;
        @CommandLine.Option(names = "--size", description = "Size", defaultValue = "1024")
        private int size;
        @CommandLine.Parameters(index = "0", description = "File", arity = "0..1")
        private String file;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "grp-4", description = "Group 4", subcommands = { PicoGrpChild4a.class, PicoGrpChild4b.class })
    public static class PicoGrpCmd4 implements Runnable {
        @CommandLine.Option(names = { "-r", "--region" }, description = "Region")
        private String region;
        @CommandLine.Option(names = "--skip", description = "Skip validation")
        private boolean skip;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "child-4a", description = "Child 4a")
    public static class PicoGrpChild4a implements Runnable {
        @CommandLine.Option(names = { "-u", "--user" }, description = "User")
        private String user;
        @CommandLine.Option(names = "--active", description = "Active")
        private boolean active;
        @CommandLine.Parameters(index = "0", description = "Role", arity = "0..1")
        private String role;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "child-4b", description = "Child 4b")
    public static class PicoGrpChild4b implements Runnable {
        @CommandLine.Option(names = { "-t", "--tag" }, description = "Tag")
        private String tag;
        @CommandLine.Option(names = "--weight", description = "Weight", defaultValue = "1")
        private int weight;
        @CommandLine.Parameters(index = "0", description = "Label", arity = "0..1")
        private String label;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "grp-5", description = "Group 5", subcommands = { PicoGrpChild5a.class, PicoGrpChild5b.class })
    public static class PicoGrpCmd5 implements Runnable {
        @CommandLine.Option(names = { "-s", "--scope" }, description = "Scope")
        private String scope;
        @CommandLine.Option(names = "--trace", description = "Trace")
        private boolean trace;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "child-5a", description = "Child 5a")
    public static class PicoGrpChild5a implements Runnable {
        @CommandLine.Option(names = { "-p", "--pattern" }, description = "Pattern")
        private String pattern;
        @CommandLine.Option(names = "--strict", description = "Strict")
        private boolean strict;
        @CommandLine.Parameters(index = "0", description = "Expression", arity = "0..1")
        private String expr;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "child-5b", description = "Child 5b")
    public static class PicoGrpChild5b implements Runnable {
        @CommandLine.Option(names = { "-c", "--codec" }, description = "Codec")
        private String codec;
        @CommandLine.Option(names = "--depth", description = "Depth", defaultValue = "3")
        private int depth;
        @CommandLine.Parameters(index = "0", description = "Data", arity = "0..1")
        private String data;

        @Override
        public void run() {
        }
    }

    // ---- Aesh OptionList / OptionGroup commands ----

    @CommandDefinition(name = "list-group-cmd", description = "Command with list and group options")
    public static class AeshListGroupCmd implements Command<CommandInvocation> {
        @OptionList(shortName = 'i', name = "items", valueSeparator = ',')
        private List<String> items;
        @OptionGroup(shortName = 'D', description = "Properties")
        private Map<String, String> properties;
        @Option(shortName = 'v', hasValue = false, description = "Verbose")
        private boolean verbose;
        @Argument(description = "Target")
        private String target;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    // ---- Aesh generateHelp / version commands ----

    @CommandDefinition(name = "help-cmd", description = "Command with generated help", generateHelp = true)
    public static class AeshHelpCmd implements Command<CommandInvocation> {
        @Option(shortName = 'n', description = "Name")
        private String name;
        @Option(shortName = 'c', description = "Count", defaultValue = "5")
        private int count;
        @Argument(description = "Target")
        private String target;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "ver-cmd", description = "Versioned command", version = "2.1.0")
    public static class AeshVersionCmd implements Command<CommandInvocation> {
        @Option(shortName = 'o', description = "Output")
        private String output;
        @Option(shortName = 'q', hasValue = false, description = "Quiet")
        private boolean quiet;
        @Argument(description = "Input file")
        private String input;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    // ---- Aesh 3-level nested group commands ----

    @GroupCommandDefinition(name = "top", description = "Top level", groupCommands = { AeshMidGroup.class })
    public static class AeshTopGroup implements Command<CommandInvocation> {
        @Option(shortName = 'g', hasValue = false, description = "Global flag")
        private boolean global;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "mid", description = "Mid level", groupCommands = { AeshLeafCmd1.class,
            AeshLeafCmd2.class })
    public static class AeshMidGroup implements Command<CommandInvocation> {
        @Option(shortName = 'r', description = "Region")
        private String region;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "leaf1", description = "Leaf command 1")
    public static class AeshLeafCmd1 implements Command<CommandInvocation> {
        @Option(shortName = 'n', description = "Name")
        private String name;
        @Option(hasValue = false, description = "Force")
        private boolean force;
        @Argument(description = "Value")
        private String value;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "leaf2", description = "Leaf command 2")
    public static class AeshLeafCmd2 implements Command<CommandInvocation> {
        @Option(shortName = 'f', description = "File path")
        private String file;
        @Option(description = "Encoding", defaultValue = "UTF-8")
        private String encoding;
        @Argument(description = "Data")
        private String data;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    // ---- Picocli OptionList / OptionGroup equivalents ----

    @CommandLine.Command(name = "list-group-cmd", description = "Command with lists and maps")
    public static class PicoListGroupCmd implements Runnable {
        @CommandLine.Option(names = { "-i", "--items" }, split = ",", description = "Items")
        private List<String> items;
        @CommandLine.Option(names = "-D", description = "Properties")
        private Map<String, String> properties;
        @CommandLine.Option(names = { "-v", "--verbose" }, description = "Verbose")
        private boolean verbose;
        @CommandLine.Parameters(index = "0", description = "Target", arity = "0..1")
        private String target;

        @Override
        public void run() {
        }
    }

    // ---- Picocli generateHelp / version equivalents ----

    @CommandLine.Command(name = "help-cmd", description = "With help", mixinStandardHelpOptions = true)
    public static class PicoHelpCmd implements Runnable {
        @CommandLine.Option(names = { "-n", "--name" }, description = "Name")
        private String name;
        @CommandLine.Option(names = { "-c", "--count" }, description = "Count", defaultValue = "5")
        private int count;
        @CommandLine.Parameters(index = "0", description = "Target", arity = "0..1")
        private String target;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "ver-cmd", description = "Versioned", version = "2.1.0", mixinStandardHelpOptions = true)
    public static class PicoVersionCmd implements Runnable {
        @CommandLine.Option(names = { "-o", "--output" }, description = "Output")
        private String output;
        @CommandLine.Option(names = { "-q", "--quiet" }, description = "Quiet")
        private boolean quiet;
        @CommandLine.Parameters(index = "0", description = "Input file", arity = "0..1")
        private String input;

        @Override
        public void run() {
        }
    }

    // ---- Picocli 3-level nested group ----

    @CommandLine.Command(name = "top", description = "Top level", subcommands = { PicoMidGroup.class })
    public static class PicoTopGroup implements Runnable {
        @CommandLine.Option(names = { "-g", "--global" }, description = "Global flag")
        private boolean global;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "mid", description = "Mid level", subcommands = { PicoLeafCmd1.class,
            PicoLeafCmd2.class })
    public static class PicoMidGroup implements Runnable {
        @CommandLine.Option(names = { "-r", "--region" }, description = "Region")
        private String region;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "leaf1", description = "Leaf 1")
    public static class PicoLeafCmd1 implements Runnable {
        @CommandLine.Option(names = { "-n", "--name" }, description = "Name")
        private String name;
        @CommandLine.Option(names = "--force", description = "Force")
        private boolean force;
        @CommandLine.Parameters(index = "0", description = "Value", arity = "0..1")
        private String value;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "leaf2", description = "Leaf 2")
    public static class PicoLeafCmd2 implements Runnable {
        @CommandLine.Option(names = { "-f", "--file" }, description = "File path")
        private String file;
        @CommandLine.Option(names = "--encoding", description = "Encoding", defaultValue = "UTF-8")
        private String encoding;
        @CommandLine.Parameters(index = "0", description = "Data", arity = "0..1")
        private String data;

        @Override
        public void run() {
        }
    }

    // ---- Aesh mixin classes ----

    public static class AeshCommonMixin {
        @Option(shortName = 'v', hasValue = false, negatable = true, description = "Verbose")
        private boolean verbose;
        @Option(shortName = 'o', description = "Output path")
        private String output;
    }

    @CommandDefinition(name = "mix-file-ops", description = "File operations with mixin")
    public static class AeshMixCmd1 implements Command<CommandInvocation> {
        @Mixin
        private AeshCommonMixin common;
        @Option(shortName = 'r', hasValue = false, negatable = true, description = "Recursive")
        private boolean recursive;
        @Option(description = "Buffer size", defaultValue = "4096")
        private int bufferSize;
        @Argument(description = "Source path")
        private String source;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mix-net-fetch", description = "Network fetch with mixin")
    public static class AeshMixCmd2 implements Command<CommandInvocation> {
        @Mixin
        private AeshCommonMixin common;
        @Option(shortName = 'u', description = "URL")
        private String url;
        @Option(description = "Timeout", defaultValue = "30")
        private int timeout;
        @Argument(description = "Destination")
        private String destination;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mix-db-query", description = "Database query with mixin")
    public static class AeshMixCmd3 implements Command<CommandInvocation> {
        @Mixin
        private AeshCommonMixin common;
        @Option(shortName = 'h', description = "Host")
        private String host;
        @Option(shortName = 'p', optionalValue = true, defaultValue = "5432", description = "Port")
        private String port;
        @Argument(description = "Query string")
        private String query;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mix-build-proj", description = "Build project with mixin")
    public static class AeshMixCmd4 implements Command<CommandInvocation> {
        @Mixin
        private AeshCommonMixin common;
        @Option(shortName = 't', description = "Target")
        private String target;
        @Option(description = "Parallelism", defaultValue = "4")
        private int jobs;
        @Argument(description = "Project dir")
        private String projectDir;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mix-deploy-app", description = "Deploy application with mixin")
    public static class AeshMixCmd5 implements Command<CommandInvocation> {
        @Mixin
        private AeshCommonMixin common;
        @Option(shortName = 'e', description = "Environment")
        private String env;
        @Option(description = "Replicas", defaultValue = "1")
        private int replicas;
        @Argument(description = "Artifact")
        private String artifact;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mix-log-search", description = "Search logs with mixin")
    public static class AeshMixCmd6 implements Command<CommandInvocation> {
        @Mixin
        private AeshCommonMixin common;
        @Option(shortName = 'f', description = "Filter pattern")
        private String filter;
        @Option(optionalValue = true, defaultValue = "100", description = "Max results")
        private String maxResults;
        @Argument(description = "Log file")
        private String logFile;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mix-user-mgmt", description = "User management with mixin")
    public static class AeshMixCmd7 implements Command<CommandInvocation> {
        @Mixin
        private AeshCommonMixin common;
        @Option(shortName = 'n', description = "Username")
        private String username;
        @Option(description = "Max sessions", defaultValue = "5")
        private int maxSessions;
        @Argument(description = "Action")
        private String action;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mix-cache-ctl", description = "Cache control with mixin")
    public static class AeshMixCmd8 implements Command<CommandInvocation> {
        @Mixin
        private AeshCommonMixin common;
        @Option(shortName = 'k', description = "Key prefix")
        private String keyPrefix;
        @Option(description = "TTL seconds", defaultValue = "3600")
        private int ttl;
        @Argument(description = "Region")
        private String region;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mix-config-edit", description = "Edit configuration with mixin")
    public static class AeshMixCmd9 implements Command<CommandInvocation> {
        @Mixin
        private AeshCommonMixin common;
        @Option(shortName = 's', description = "Section")
        private String section;
        @Option(description = "Indent level", defaultValue = "2")
        private int indent;
        @Argument(description = "Config file")
        private String configFile;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mix-report-gen", description = "Generate reports with mixin")
    public static class AeshMixCmd10 implements Command<CommandInvocation> {
        @Mixin
        private AeshCommonMixin common;
        @Option(shortName = 'f', description = "Format")
        private String format;
        @Option(description = "Page limit", defaultValue = "50")
        private int pageLimit;
        @Argument(description = "Report name")
        private String reportName;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    // ---- Picocli mixin classes ----

    public static class PicoCommonMixin {
        @CommandLine.Option(names = { "-v", "--verbose" }, description = "Verbose")
        private boolean verbose;
        @CommandLine.Option(names = { "-o", "--output" }, description = "Output path")
        private String output;
    }

    @CommandLine.Command(name = "mix-file-ops", description = "File operations with mixin")
    public static class PicoMixCmd1 implements Runnable {
        @CommandLine.Mixin
        private PicoCommonMixin common;
        @CommandLine.Option(names = { "-r", "--recursive" }, description = "Recursive")
        private boolean recursive;
        @CommandLine.Option(names = "--buffer-size", description = "Buffer size", defaultValue = "4096")
        private int bufferSize;
        @CommandLine.Parameters(index = "0", description = "Source path", arity = "0..1")
        private String source;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "mix-net-fetch", description = "Network fetch with mixin")
    public static class PicoMixCmd2 implements Runnable {
        @CommandLine.Mixin
        private PicoCommonMixin common;
        @CommandLine.Option(names = { "-u", "--url" }, description = "URL")
        private String url;
        @CommandLine.Option(names = "--timeout", description = "Timeout", defaultValue = "30")
        private int timeout;
        @CommandLine.Parameters(index = "0", description = "Destination", arity = "0..1")
        private String destination;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "mix-db-query", description = "Database query with mixin")
    public static class PicoMixCmd3 implements Runnable {
        @CommandLine.Mixin
        private PicoCommonMixin common;
        @CommandLine.Option(names = { "-h", "--host" }, description = "Host")
        private String host;
        @CommandLine.Option(names = { "-p", "--port" }, description = "Port", defaultValue = "5432")
        private int port;
        @CommandLine.Parameters(index = "0", description = "Query string", arity = "0..1")
        private String query;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "mix-build-proj", description = "Build project with mixin")
    public static class PicoMixCmd4 implements Runnable {
        @CommandLine.Mixin
        private PicoCommonMixin common;
        @CommandLine.Option(names = { "-t", "--target" }, description = "Target")
        private String target;
        @CommandLine.Option(names = "--jobs", description = "Parallelism", defaultValue = "4")
        private int jobs;
        @CommandLine.Parameters(index = "0", description = "Project dir", arity = "0..1")
        private String projectDir;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "mix-deploy-app", description = "Deploy application with mixin")
    public static class PicoMixCmd5 implements Runnable {
        @CommandLine.Mixin
        private PicoCommonMixin common;
        @CommandLine.Option(names = { "-e", "--env" }, description = "Environment")
        private String env;
        @CommandLine.Option(names = "--replicas", description = "Replicas", defaultValue = "1")
        private int replicas;
        @CommandLine.Parameters(index = "0", description = "Artifact", arity = "0..1")
        private String artifact;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "mix-log-search", description = "Search logs with mixin")
    public static class PicoMixCmd6 implements Runnable {
        @CommandLine.Mixin
        private PicoCommonMixin common;
        @CommandLine.Option(names = { "-f", "--filter" }, description = "Filter pattern")
        private String filter;
        @CommandLine.Option(names = "--max-results", description = "Max results", defaultValue = "100")
        private int maxResults;
        @CommandLine.Parameters(index = "0", description = "Log file", arity = "0..1")
        private String logFile;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "mix-user-mgmt", description = "User management with mixin")
    public static class PicoMixCmd7 implements Runnable {
        @CommandLine.Mixin
        private PicoCommonMixin common;
        @CommandLine.Option(names = { "-n", "--username" }, description = "Username")
        private String username;
        @CommandLine.Option(names = "--max-sessions", description = "Max sessions", defaultValue = "5")
        private int maxSessions;
        @CommandLine.Parameters(index = "0", description = "Action", arity = "0..1")
        private String action;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "mix-cache-ctl", description = "Cache control with mixin")
    public static class PicoMixCmd8 implements Runnable {
        @CommandLine.Mixin
        private PicoCommonMixin common;
        @CommandLine.Option(names = { "-k", "--key-prefix" }, description = "Key prefix")
        private String keyPrefix;
        @CommandLine.Option(names = "--ttl", description = "TTL seconds", defaultValue = "3600")
        private int ttl;
        @CommandLine.Parameters(index = "0", description = "Region", arity = "0..1")
        private String region;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "mix-config-edit", description = "Edit configuration with mixin")
    public static class PicoMixCmd9 implements Runnable {
        @CommandLine.Mixin
        private PicoCommonMixin common;
        @CommandLine.Option(names = { "-s", "--section" }, description = "Section")
        private String section;
        @CommandLine.Option(names = "--indent", description = "Indent level", defaultValue = "2")
        private int indent;
        @CommandLine.Parameters(index = "0", description = "Config file", arity = "0..1")
        private String configFile;

        @Override
        public void run() {
        }
    }

    @CommandLine.Command(name = "mix-report-gen", description = "Generate reports with mixin")
    public static class PicoMixCmd10 implements Runnable {
        @CommandLine.Mixin
        private PicoCommonMixin common;
        @CommandLine.Option(names = { "-f", "--format" }, description = "Format")
        private String format;
        @CommandLine.Option(names = "--page-limit", description = "Page limit", defaultValue = "50")
        private int pageLimit;
        @CommandLine.Parameters(index = "0", description = "Report name", arity = "0..1")
        private String reportName;

        @Override
        public void run() {
        }
    }

    // ---- Command class arrays for easy indexing ----

    private static final Class<? extends Command<CommandInvocation>>[] AESH_COMMANDS = new Class[] {
            AeshCmd1.class, AeshCmd2.class, AeshCmd3.class, AeshCmd4.class, AeshCmd5.class,
            AeshCmd6.class, AeshCmd7.class, AeshCmd8.class, AeshCmd9.class, AeshCmd10.class
    };

    private static final Class<? extends Runnable>[] PICO_COMMANDS = new Class[] {
            PicoCmd1.class, PicoCmd2.class, PicoCmd3.class, PicoCmd4.class, PicoCmd5.class,
            PicoCmd6.class, PicoCmd7.class, PicoCmd8.class, PicoCmd9.class, PicoCmd10.class
    };

    private static final Class<? extends Command<CommandInvocation>>[] AESH_GROUP_COMMANDS = new Class[] {
            AeshGrpCmd1.class, AeshGrpCmd2.class, AeshGrpCmd3.class, AeshGrpCmd4.class, AeshGrpCmd5.class
    };

    private static final Class<? extends Runnable>[] PICO_GROUP_COMMANDS = new Class[] {
            PicoGrpCmd1.class, PicoGrpCmd2.class, PicoGrpCmd3.class, PicoGrpCmd4.class, PicoGrpCmd5.class
    };

    private static final Class<? extends Command<CommandInvocation>>[] AESH_MIXIN_COMMANDS = new Class[] {
            AeshMixCmd1.class, AeshMixCmd2.class, AeshMixCmd3.class, AeshMixCmd4.class, AeshMixCmd5.class,
            AeshMixCmd6.class, AeshMixCmd7.class, AeshMixCmd8.class, AeshMixCmd9.class, AeshMixCmd10.class
    };

    private static final Class<? extends Runnable>[] PICO_MIXIN_COMMANDS = new Class[] {
            PicoMixCmd1.class, PicoMixCmd2.class, PicoMixCmd3.class, PicoMixCmd4.class, PicoMixCmd5.class,
            PicoMixCmd6.class, PicoMixCmd7.class, PicoMixCmd8.class, PicoMixCmd9.class, PicoMixCmd10.class
    };

    private static final Class<? extends Command<CommandInvocation>>[] AESH_VARIETY_COMMANDS = new Class[] {
            AeshListGroupCmd.class, AeshHelpCmd.class, AeshVersionCmd.class
    };

    private static final Class<? extends Runnable>[] PICO_VARIETY_COMMANDS = new Class[] {
            PicoListGroupCmd.class, PicoHelpCmd.class, PicoVersionCmd.class
    };

    // ---- Benchmark runner ----

    public static void main(String[] args) throws Exception {
        StartupBenchmark benchmark = new StartupBenchmark();
        benchmark.runFlatBenchmark();
        System.out.println();
        benchmark.runMixinBenchmark();
        System.out.println();
        benchmark.runGroupBenchmark();
        System.out.println();
        benchmark.runNestedGroupBenchmark();
        System.out.println();
        benchmark.runOptionVarietyBenchmark();
        System.out.println();
        benchmark.runParsingBenchmark();
        System.out.println();
        benchmark.runCompletionBenchmark();
    }

    public void runFlatBenchmark() throws Exception {
        System.out.println("=== Flat Command Benchmark ===");
        System.out.println("Warmup: " + WARMUP_ITERATIONS + ", Measured: " + MEASURED_ITERATIONS + " iterations");
        System.out.println();

        double[] genResults = new double[COMMAND_COUNTS.length];
        double[] reflResults = new double[COMMAND_COUNTS.length];
        double[] picoProgResults = new double[COMMAND_COUNTS.length];
        double[] picoReflResults = new double[COMMAND_COUNTS.length];

        for (int c = 0; c < COMMAND_COUNTS.length; c++) {
            int count = COMMAND_COUNTS[c];

            // Benchmark Aesh (generated / processor path)
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildAeshRuntimeGenerated(count);
            }
            long genNs = timeIterations(() -> buildAeshRuntimeGenerated(count));
            genResults[c] = (genNs / (double) MEASURED_ITERATIONS) / 1000.0;

            // Benchmark Aesh (reflection path)
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildAeshRuntimeReflection(count);
            }
            long reflNs = timeIterations(() -> buildAeshRuntimeReflection(count));
            reflResults[c] = (reflNs / (double) MEASURED_ITERATIONS) / 1000.0;

            // Benchmark Picocli (programmatic path)
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildPicocliRuntimeProgrammatic(count);
            }
            long picoProgNs = timeIterations(() -> buildPicocliRuntimeProgrammatic(count));
            picoProgResults[c] = (picoProgNs / (double) MEASURED_ITERATIONS) / 1000.0;

            // Benchmark Picocli (reflection path)
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildPicocliRuntimeReflection(count);
            }
            long picoReflNs = timeIterations(() -> buildPicocliRuntimeReflection(count));
            picoReflResults[c] = (picoReflNs / (double) MEASURED_ITERATIONS) / 1000.0;
        }

        // Print summary table
        System.out.printf("Commands | %14s | %14s | %14s | %14s | Refl/Gen | PicoR/Gen%n",
                "Aesh gen (us)", "Aesh refl (us)", "Pico prog (us)", "Pico refl (us)");
        System.out.println("---------|-" + "-".repeat(14) + "-|-" + "-".repeat(14) + "-|-"
                + "-".repeat(14) + "-|-" + "-".repeat(14) + "-|----------|----------");
        for (int c = 0; c < COMMAND_COUNTS.length; c++) {
            double reflGenRatio = genResults[c] != 0 ? reflResults[c] / genResults[c] : 0;
            double picoReflGenRatio = genResults[c] != 0 ? picoReflResults[c] / genResults[c] : 0;
            System.out.printf("%8d | %14.1f | %14.1f | %14.1f | %14.1f | %7.2fx | %8.2fx%n",
                    COMMAND_COUNTS[c], genResults[c], reflResults[c],
                    picoProgResults[c], picoReflResults[c],
                    reflGenRatio, picoReflGenRatio);
        }
    }

    public void runMixinBenchmark() throws Exception {
        System.out.println("=== Mixin Command Benchmark ===");
        System.out.println("Each command uses a shared @Mixin with 2 options + 2 own options + argument");
        System.out.println("Compare with Flat benchmark (same option count, no mixin indirection)");
        System.out.println("Warmup: " + WARMUP_ITERATIONS + ", Measured: " + MEASURED_ITERATIONS + " iterations");
        System.out.println();

        double[] genResults = new double[COMMAND_COUNTS.length];
        double[] reflResults = new double[COMMAND_COUNTS.length];
        double[] picoProgResults = new double[COMMAND_COUNTS.length];
        double[] picoReflResults = new double[COMMAND_COUNTS.length];

        for (int c = 0; c < COMMAND_COUNTS.length; c++) {
            int count = COMMAND_COUNTS[c];

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildAeshRuntimeMixinGenerated(count);
            }
            long genNs = timeIterations(() -> buildAeshRuntimeMixinGenerated(count));
            genResults[c] = (genNs / (double) MEASURED_ITERATIONS) / 1000.0;

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildAeshRuntimeMixinReflection(count);
            }
            long reflNs = timeIterations(() -> buildAeshRuntimeMixinReflection(count));
            reflResults[c] = (reflNs / (double) MEASURED_ITERATIONS) / 1000.0;

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildPicocliRuntimeMixinProgrammatic(count);
            }
            long picoProgNs = timeIterations(() -> buildPicocliRuntimeMixinProgrammatic(count));
            picoProgResults[c] = (picoProgNs / (double) MEASURED_ITERATIONS) / 1000.0;

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildPicocliRuntimeMixinReflection(count);
            }
            long picoReflNs = timeIterations(() -> buildPicocliRuntimeMixinReflection(count));
            picoReflResults[c] = (picoReflNs / (double) MEASURED_ITERATIONS) / 1000.0;
        }

        System.out.printf("Commands | %14s | %14s | %14s | %14s | Refl/Gen | PicoR/Gen%n",
                "Aesh gen (us)", "Aesh refl (us)", "Pico prog (us)", "Pico refl (us)");
        System.out.println("---------|-" + "-".repeat(14) + "-|-" + "-".repeat(14) + "-|-"
                + "-".repeat(14) + "-|-" + "-".repeat(14) + "-|----------|----------");
        for (int c = 0; c < COMMAND_COUNTS.length; c++) {
            double reflGenRatio = genResults[c] != 0 ? reflResults[c] / genResults[c] : 0;
            double picoReflGenRatio = genResults[c] != 0 ? picoReflResults[c] / genResults[c] : 0;
            System.out.printf("%8d | %14.1f | %14.1f | %14.1f | %14.1f | %7.2fx | %8.2fx%n",
                    COMMAND_COUNTS[c], genResults[c], reflResults[c],
                    picoProgResults[c], picoReflResults[c],
                    reflGenRatio, picoReflGenRatio);
        }
    }

    // ---- Aesh: generated / annotation-processor path (mixin) ----

    private void buildAeshRuntimeMixinGenerated(int commandCount) throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        for (int i = 0; i < commandCount; i++) {
            registry.addCommand(createGeneratedMixinContainer("mixcmd-" + i, i));
        }
        AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();
    }

    private CommandContainer<CommandInvocation> createGeneratedMixinContainer(String name, int index) throws Exception {
        Command<CommandInvocation> cmd = AESH_MIXIN_COMMANDS[index % AESH_MIXIN_COMMANDS.length]
                .getDeclaredConstructor().newInstance();
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> pc = ProcessedCommandBuilder
                .<Command<CommandInvocation>, CommandInvocation> builder()
                .name(name)
                .description("Mixin command " + name)
                .command(cmd)
                // Mixin-sourced options (tagged with mixinFieldName)
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('v')
                        .name("verbose")
                        .description("Verbose")
                        .type(boolean.class)
                        .fieldName("verbose")
                        .optionType(OptionType.BOOLEAN)
                        .negatable(true)
                        .mixinFieldName("common")
                        .build())
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('o')
                        .name("output")
                        .description("Output path")
                        .type(String.class)
                        .fieldName("output")
                        .optionType(OptionType.NORMAL)
                        .mixinFieldName("common")
                        .build())
                // Command's own options
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('r')
                        .name("recursive")
                        .description("Recursive")
                        .type(boolean.class)
                        .fieldName("recursive")
                        .optionType(OptionType.BOOLEAN)
                        .build())
                .addOption(ProcessedOptionBuilder.builder()
                        .name("bufferSize")
                        .description("Buffer size")
                        .type(int.class)
                        .fieldName("bufferSize")
                        .optionType(OptionType.NORMAL)
                        .addDefaultValue("4096")
                        .build())
                .create();
        pc.setArgument(ProcessedOptionBuilder.builder()
                .shortName('\u0000')
                .name("")
                .description("Source path")
                .type(String.class)
                .fieldName("source")
                .optionType(OptionType.ARGUMENT)
                .build());
        return new AeshCommandContainer<>(pc);
    }

    // ---- Aesh: reflection path (mixin) ----

    private void buildAeshRuntimeMixinReflection(int commandCount) throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        AeshCommandContainerBuilder<CommandInvocation> containerBuilder = new AeshCommandContainerBuilder<>();

        int annotatedCount = Math.min(commandCount, AESH_MIXIN_COMMANDS.length);
        for (int i = 0; i < annotatedCount; i++) {
            Command<CommandInvocation> cmd = AESH_MIXIN_COMMANDS[i].getDeclaredConstructor().newInstance();
            CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) REFLECTION_CREATE
                    .invoke(containerBuilder, cmd);
            registry.addCommand(container);
        }

        for (int i = AESH_MIXIN_COMMANDS.length; i < commandCount; i++) {
            registry.addCommand(createGeneratedMixinContainer("mixcmd-" + i, i));
        }

        AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();
    }

    // ---- Picocli: programmatic path (mixin) ----

    private void buildPicocliRuntimeMixinProgrammatic(int commandCount) {
        CommandSpec mainSpec = CommandSpec.create();
        mainSpec.name("main");
        CommandLine cmd = new CommandLine(mainSpec);
        for (int i = 0; i < commandCount; i++) {
            cmd.addSubcommand("mixcmd-" + i, new CommandLine(createPicocliProgrammaticMixinCommand("mixcmd-" + i)));
        }
    }

    private CommandSpec createPicocliProgrammaticMixinCommand(String name) {
        CommandSpec spec = CommandSpec.create();
        spec.name(name);
        // Same options as mixin + own (picocli programmatic has no mixin indirection)
        spec.addOption(OptionSpec.builder("-v", "--verbose")
                .type(boolean.class)
                .description("Verbose")
                .build());
        spec.addOption(OptionSpec.builder("-o", "--output")
                .type(String.class)
                .description("Output path")
                .build());
        spec.addOption(OptionSpec.builder("-r", "--recursive")
                .type(boolean.class)
                .description("Recursive")
                .build());
        spec.addOption(OptionSpec.builder("--buffer-size")
                .type(int.class)
                .description("Buffer size")
                .defaultValue("4096")
                .build());
        spec.addPositional(PositionalParamSpec.builder()
                .type(String.class)
                .description("Source path")
                .build());
        return spec;
    }

    // ---- Picocli: reflection path (mixin) ----

    private void buildPicocliRuntimeMixinReflection(int commandCount) {
        CommandLine cmd = new CommandLine(new PicocliMain());
        for (int i = 0; i < commandCount; i++) {
            Class<? extends Runnable> picoClass = PICO_MIXIN_COMMANDS[i % PICO_MIXIN_COMMANDS.length];
            try {
                cmd.addSubcommand("mixcmd-" + i, picoClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void runGroupBenchmark() throws Exception {
        System.out.println("=== Group Command Benchmark ===");
        System.out.println("Each group = 1 parent + 2 child subcommands");
        System.out.println("Warmup: " + WARMUP_ITERATIONS + ", Measured: " + MEASURED_ITERATIONS + " iterations");
        System.out.println();

        double[] genResults = new double[COMMAND_COUNTS.length];
        double[] reflResults = new double[COMMAND_COUNTS.length];
        double[] picoProgResults = new double[COMMAND_COUNTS.length];
        double[] picoReflResults = new double[COMMAND_COUNTS.length];

        for (int c = 0; c < COMMAND_COUNTS.length; c++) {
            int count = COMMAND_COUNTS[c];

            // Benchmark Aesh (generated / processor path)
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildAeshRuntimeGroupGenerated(count);
            }
            long genNs = timeIterations(() -> buildAeshRuntimeGroupGenerated(count));
            genResults[c] = (genNs / (double) MEASURED_ITERATIONS) / 1000.0;

            // Benchmark Aesh (reflection path)
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildAeshRuntimeGroupReflection(count);
            }
            long reflNs = timeIterations(() -> buildAeshRuntimeGroupReflection(count));
            reflResults[c] = (reflNs / (double) MEASURED_ITERATIONS) / 1000.0;

            // Benchmark Picocli (programmatic path)
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildPicocliRuntimeGroupProgrammatic(count);
            }
            long picoProgNs = timeIterations(() -> buildPicocliRuntimeGroupProgrammatic(count));
            picoProgResults[c] = (picoProgNs / (double) MEASURED_ITERATIONS) / 1000.0;

            // Benchmark Picocli (reflection path)
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildPicocliRuntimeGroupReflection(count);
            }
            long picoReflNs = timeIterations(() -> buildPicocliRuntimeGroupReflection(count));
            picoReflResults[c] = (picoReflNs / (double) MEASURED_ITERATIONS) / 1000.0;
        }

        // Print summary table
        System.out.printf("Groups   | %14s | %14s | %14s | %14s | Refl/Gen | PicoR/Gen%n",
                "Aesh gen (us)", "Aesh refl (us)", "Pico prog (us)", "Pico refl (us)");
        System.out.println("---------|-" + "-".repeat(14) + "-|-" + "-".repeat(14) + "-|-"
                + "-".repeat(14) + "-|-" + "-".repeat(14) + "-|----------|----------");
        for (int c = 0; c < COMMAND_COUNTS.length; c++) {
            double reflGenRatio = genResults[c] != 0 ? reflResults[c] / genResults[c] : 0;
            double picoReflGenRatio = genResults[c] != 0 ? picoReflResults[c] / genResults[c] : 0;
            System.out.printf("%8d | %14.1f | %14.1f | %14.1f | %14.1f | %7.2fx | %8.2fx%n",
                    COMMAND_COUNTS[c], genResults[c], reflResults[c],
                    picoProgResults[c], picoReflResults[c],
                    reflGenRatio, picoReflGenRatio);
        }
    }

    // ---- Aesh: generated / annotation-processor path (flat) ----

    private void buildAeshRuntimeGenerated(int commandCount) throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        for (int i = 0; i < commandCount; i++) {
            registry.addCommand(createGeneratedContainer("cmd-" + i, i));
        }
        AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();
    }

    private CommandContainer<CommandInvocation> createGeneratedContainer(String name, int index) throws Exception {
        Command<CommandInvocation> cmd = AESH_COMMANDS[index % AESH_COMMANDS.length]
                .getDeclaredConstructor().newInstance();
        boolean useStopAtFirst = (index % 3 == 0);
        boolean useDefaultValueProvider = (index % 5 == 0);
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> builder = ProcessedCommandBuilder
                .<Command<CommandInvocation>, CommandInvocation> builder()
                .name(name)
                .description("Command " + name)
                .command(cmd)
                .stopAtFirstPositional(useStopAtFirst)
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('o')
                        .name("output")
                        .description("Output path")
                        .type(String.class)
                        .fieldName("output")
                        .optionType(OptionType.NORMAL)
                        .optionalValue(true)
                        .addDefaultValue("/tmp/out")
                        .build())
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('r')
                        .name("recursive")
                        .description("Recursive")
                        .type(boolean.class)
                        .fieldName("recursive")
                        .optionType(OptionType.BOOLEAN)
                        .negatable(true)
                        .build())
                .addOption(ProcessedOptionBuilder.builder()
                        .name("bufferSize")
                        .description("Buffer size")
                        .type(int.class)
                        .fieldName("bufferSize")
                        .optionType(OptionType.NORMAL)
                        .addDefaultValue("4096")
                        .build());
        if (useDefaultValueProvider) {
            builder.defaultValueProvider(new BenchmarkDefaultValueProvider());
        }
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> pc = builder.create();
        pc.setArgument(ProcessedOptionBuilder.builder()
                .shortName('\u0000')
                .name("")
                .description("Source path")
                .type(String.class)
                .fieldName("source")
                .optionType(OptionType.ARGUMENT)
                .build());
        return new AeshCommandContainer<>(pc);
    }

    // ---- Aesh: reflection path (flat) ----

    private void buildAeshRuntimeReflection(int commandCount) throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        AeshCommandContainerBuilder<CommandInvocation> containerBuilder = new AeshCommandContainerBuilder<>();

        int annotatedCount = Math.min(commandCount, AESH_COMMANDS.length);
        for (int i = 0; i < annotatedCount; i++) {
            Command<CommandInvocation> cmd = AESH_COMMANDS[i].getDeclaredConstructor().newInstance();
            CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) REFLECTION_CREATE
                    .invoke(containerBuilder, cmd);
            registry.addCommand(container);
        }

        // For counts > 10, use ProcessedCommandBuilder (same as generated path)
        for (int i = AESH_COMMANDS.length; i < commandCount; i++) {
            registry.addCommand(createGeneratedContainer("cmd-" + i, i));
        }

        AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();
    }

    // ---- Picocli: reflection path (flat) ----

    private void buildPicocliRuntimeReflection(int commandCount) {
        CommandLine cmd = new CommandLine(new PicocliMain());
        for (int i = 0; i < commandCount; i++) {
            Class<? extends Runnable> picoClass = PICO_COMMANDS[i % PICO_COMMANDS.length];
            try {
                cmd.addSubcommand("cmd-" + i, picoClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // ---- Picocli: programmatic path (flat) ----

    private void buildPicocliRuntimeProgrammatic(int commandCount) {
        CommandSpec mainSpec = CommandSpec.create();
        mainSpec.name("main");
        CommandLine cmd = new CommandLine(mainSpec);
        for (int i = 0; i < commandCount; i++) {
            cmd.addSubcommand("cmd-" + i, new CommandLine(createPicocliProgrammaticCommand("cmd-" + i)));
        }
    }

    private CommandSpec createPicocliProgrammaticCommand(String name) {
        CommandSpec spec = CommandSpec.create();
        spec.name(name);
        spec.addOption(OptionSpec.builder("-o", "--output")
                .type(String.class)
                .description("Output path")
                .build());
        spec.addOption(OptionSpec.builder("-r", "--recursive")
                .type(boolean.class)
                .description("Recursive")
                .build());
        spec.addOption(OptionSpec.builder("--buffer-size")
                .type(int.class)
                .description("Buffer size")
                .defaultValue("4096")
                .build());
        spec.addPositional(PositionalParamSpec.builder()
                .type(String.class)
                .description("Source path")
                .build());
        return spec;
    }

    // ---- Aesh: generated / annotation-processor path (groups) ----

    private void buildAeshRuntimeGroupGenerated(int groupCount) throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        for (int i = 0; i < groupCount; i++) {
            registry.addCommand(createGeneratedGroupContainer("grp-" + i, i));
        }
        AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();
    }

    private CommandContainer<CommandInvocation> createGeneratedGroupContainer(String name, int index) throws Exception {
        // Create parent command
        Command<CommandInvocation> parentCmd = AESH_COMMANDS[index % AESH_COMMANDS.length]
                .getDeclaredConstructor().newInstance();
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> parentPc = ProcessedCommandBuilder
                .<Command<CommandInvocation>, CommandInvocation> builder()
                .name(name)
                .description("Group " + name)
                .command(parentCmd)
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('f')
                        .name("format")
                        .description("Format")
                        .type(String.class)
                        .fieldName("output")
                        .optionType(OptionType.NORMAL)
                        .inherited(true)
                        .build())
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('v')
                        .name("verbose")
                        .description("Verbose")
                        .type(boolean.class)
                        .fieldName("recursive")
                        .optionType(OptionType.BOOLEAN)
                        .inherited(true)
                        .negatable(true)
                        .build())
                .create();
        AeshCommandContainer<CommandInvocation> parentContainer = new AeshCommandContainer<>(parentPc);

        // Create child A
        Command<CommandInvocation> childACmd = AESH_COMMANDS[(index + 1) % AESH_COMMANDS.length]
                .getDeclaredConstructor().newInstance();
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> childAPc = ProcessedCommandBuilder
                .<Command<CommandInvocation>, CommandInvocation> builder()
                .name(name + "-childA")
                .description("Child A of " + name)
                .command(childACmd)
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('i')
                        .name("input")
                        .description("Input")
                        .type(String.class)
                        .fieldName("output")
                        .optionType(OptionType.NORMAL)
                        .build())
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('d')
                        .name("debug")
                        .description("Debug")
                        .type(boolean.class)
                        .fieldName("recursive")
                        .optionType(OptionType.BOOLEAN)
                        .build())
                .create();
        childAPc.setArgument(ProcessedOptionBuilder.builder()
                .shortName('\u0000')
                .name("")
                .description("Source")
                .type(String.class)
                .fieldName("source")
                .optionType(OptionType.ARGUMENT)
                .build());
        AeshCommandContainer<CommandInvocation> childAContainer = new AeshCommandContainer<>(childAPc);

        // Create child B
        Command<CommandInvocation> childBCmd = AESH_COMMANDS[(index + 2) % AESH_COMMANDS.length]
                .getDeclaredConstructor().newInstance();
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> childBPc = ProcessedCommandBuilder
                .<Command<CommandInvocation>, CommandInvocation> builder()
                .name(name + "-childB")
                .description("Child B of " + name)
                .command(childBCmd)
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('n')
                        .name("name")
                        .description("Name")
                        .type(String.class)
                        .fieldName("output")
                        .optionType(OptionType.NORMAL)
                        .build())
                .addOption(ProcessedOptionBuilder.builder()
                        .name("count")
                        .description("Count")
                        .type(int.class)
                        .fieldName("bufferSize")
                        .optionType(OptionType.NORMAL)
                        .addDefaultValue("10")
                        .build())
                .create();
        childBPc.setArgument(ProcessedOptionBuilder.builder()
                .shortName('\u0000')
                .name("")
                .description("Target")
                .type(String.class)
                .fieldName("source")
                .optionType(OptionType.ARGUMENT)
                .build());
        AeshCommandContainer<CommandInvocation> childBContainer = new AeshCommandContainer<>(childBPc);

        // Wire children to parent
        parentContainer.addChild(childAContainer);
        parentContainer.addChild(childBContainer);

        return parentContainer;
    }

    // ---- Aesh: reflection path (groups) ----

    private void buildAeshRuntimeGroupReflection(int groupCount) throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        AeshCommandContainerBuilder<CommandInvocation> containerBuilder = new AeshCommandContainerBuilder<>();

        int annotatedCount = Math.min(groupCount, AESH_GROUP_COMMANDS.length);
        for (int i = 0; i < annotatedCount; i++) {
            Command<CommandInvocation> cmd = AESH_GROUP_COMMANDS[i].getDeclaredConstructor().newInstance();
            CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) REFLECTION_CREATE
                    .invoke(containerBuilder, cmd);
            registry.addCommand(container);
        }

        // For counts > 5, use ProcessedCommandBuilder (same as generated path)
        for (int i = AESH_GROUP_COMMANDS.length; i < groupCount; i++) {
            registry.addCommand(createGeneratedGroupContainer("grp-" + i, i));
        }

        AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();
    }

    // ---- Picocli: programmatic path (groups) ----

    private void buildPicocliRuntimeGroupProgrammatic(int groupCount) {
        CommandSpec mainSpec = CommandSpec.create();
        mainSpec.name("main");
        CommandLine cmd = new CommandLine(mainSpec);
        for (int i = 0; i < groupCount; i++) {
            cmd.addSubcommand("grp-" + i, createPicocliProgrammaticGroup("grp-" + i));
        }
    }

    private CommandLine createPicocliProgrammaticGroup(String name) {
        // Parent
        CommandSpec parentSpec = CommandSpec.create();
        parentSpec.name(name);
        parentSpec.addOption(OptionSpec.builder("-f", "--format")
                .type(String.class)
                .description("Format")
                .build());
        parentSpec.addOption(OptionSpec.builder("-v", "--verbose")
                .type(boolean.class)
                .description("Verbose")
                .build());
        CommandLine parent = new CommandLine(parentSpec);

        // Child A
        CommandSpec childASpec = CommandSpec.create();
        childASpec.name(name + "-childA");
        childASpec.addOption(OptionSpec.builder("-i", "--input")
                .type(String.class)
                .description("Input")
                .build());
        childASpec.addOption(OptionSpec.builder("-d", "--debug")
                .type(boolean.class)
                .description("Debug")
                .build());
        childASpec.addPositional(PositionalParamSpec.builder()
                .type(String.class)
                .description("Source")
                .build());
        parent.addSubcommand(name + "-childA", new CommandLine(childASpec));

        // Child B
        CommandSpec childBSpec = CommandSpec.create();
        childBSpec.name(name + "-childB");
        childBSpec.addOption(OptionSpec.builder("-n", "--name")
                .type(String.class)
                .description("Name")
                .build());
        childBSpec.addOption(OptionSpec.builder("--count")
                .type(int.class)
                .description("Count")
                .defaultValue("10")
                .build());
        childBSpec.addPositional(PositionalParamSpec.builder()
                .type(String.class)
                .description("Target")
                .build());
        parent.addSubcommand(name + "-childB", new CommandLine(childBSpec));

        return parent;
    }

    // ---- Picocli: reflection path (groups) ----

    private void buildPicocliRuntimeGroupReflection(int groupCount) {
        CommandLine cmd = new CommandLine(new PicocliMain());
        for (int i = 0; i < groupCount; i++) {
            Class<? extends Runnable> grpClass = PICO_GROUP_COMMANDS[i % PICO_GROUP_COMMANDS.length];
            try {
                cmd.addSubcommand("grp-" + i, grpClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // ---- Nested group benchmark (3-level hierarchy) ----

    public void runNestedGroupBenchmark() throws Exception {
        System.out.println("=== Nested Group Benchmark (3-level: top > mid > 2 leaves) ===");
        System.out.println("Each nested group = 1 top + 1 mid + 2 leaf commands");
        System.out.println("Warmup: " + WARMUP_ITERATIONS + ", Measured: " + MEASURED_ITERATIONS + " iterations");
        System.out.println();

        double[] genResults = new double[COMMAND_COUNTS.length];
        double[] reflResults = new double[COMMAND_COUNTS.length];
        double[] picoProgResults = new double[COMMAND_COUNTS.length];
        double[] picoReflResults = new double[COMMAND_COUNTS.length];

        for (int c = 0; c < COMMAND_COUNTS.length; c++) {
            int count = COMMAND_COUNTS[c];

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildAeshNestedGenerated(count);
            }
            long genNs = timeIterations(() -> buildAeshNestedGenerated(count));
            genResults[c] = (genNs / (double) MEASURED_ITERATIONS) / 1000.0;

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildAeshNestedReflection(count);
            }
            long reflNs = timeIterations(() -> buildAeshNestedReflection(count));
            reflResults[c] = (reflNs / (double) MEASURED_ITERATIONS) / 1000.0;

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildPicocliNestedProgrammatic(count);
            }
            long picoProgNs = timeIterations(() -> buildPicocliNestedProgrammatic(count));
            picoProgResults[c] = (picoProgNs / (double) MEASURED_ITERATIONS) / 1000.0;

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildPicocliNestedReflection(count);
            }
            long picoReflNs = timeIterations(() -> buildPicocliNestedReflection(count));
            picoReflResults[c] = (picoReflNs / (double) MEASURED_ITERATIONS) / 1000.0;
        }

        System.out.printf("Nested   | %14s | %14s | %14s | %14s | Refl/Gen | PicoR/Gen%n",
                "Aesh gen (us)", "Aesh refl (us)", "Pico prog (us)", "Pico refl (us)");
        System.out.println("---------|-" + "-".repeat(14) + "-|-" + "-".repeat(14) + "-|-"
                + "-".repeat(14) + "-|-" + "-".repeat(14) + "-|----------|----------");
        for (int c = 0; c < COMMAND_COUNTS.length; c++) {
            double reflGenRatio = genResults[c] != 0 ? reflResults[c] / genResults[c] : 0;
            double picoReflGenRatio = genResults[c] != 0 ? picoReflResults[c] / genResults[c] : 0;
            System.out.printf("%8d | %14.1f | %14.1f | %14.1f | %14.1f | %7.2fx | %8.2fx%n",
                    COMMAND_COUNTS[c], genResults[c], reflResults[c],
                    picoProgResults[c], picoReflResults[c],
                    reflGenRatio, picoReflGenRatio);
        }
    }

    private void buildAeshNestedGenerated(int count) throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        for (int i = 0; i < count; i++) {
            registry.addCommand(createGeneratedNestedGroupContainer("nested-" + i, i));
        }
        AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();
    }

    private CommandContainer<CommandInvocation> createGeneratedNestedGroupContainer(String name, int index)
            throws Exception {
        // Top-level group
        Command<CommandInvocation> topCmd = AESH_COMMANDS[index % AESH_COMMANDS.length]
                .getDeclaredConstructor().newInstance();
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> topPc = ProcessedCommandBuilder
                .<Command<CommandInvocation>, CommandInvocation> builder()
                .name(name)
                .description("Top " + name)
                .command(topCmd)
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('g')
                        .name("global")
                        .description("Global flag")
                        .type(boolean.class)
                        .fieldName("recursive")
                        .optionType(OptionType.BOOLEAN)
                        .build())
                .create();
        AeshCommandContainer<CommandInvocation> topContainer = new AeshCommandContainer<>(topPc);

        // Mid-level group
        Command<CommandInvocation> midCmd = AESH_COMMANDS[(index + 1) % AESH_COMMANDS.length]
                .getDeclaredConstructor().newInstance();
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> midPc = ProcessedCommandBuilder
                .<Command<CommandInvocation>, CommandInvocation> builder()
                .name(name + "-mid")
                .description("Mid " + name)
                .command(midCmd)
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('r')
                        .name("region")
                        .description("Region")
                        .type(String.class)
                        .fieldName("output")
                        .optionType(OptionType.NORMAL)
                        .build())
                .create();
        AeshCommandContainer<CommandInvocation> midContainer = new AeshCommandContainer<>(midPc);

        // Leaf 1
        Command<CommandInvocation> leaf1Cmd = AESH_COMMANDS[(index + 2) % AESH_COMMANDS.length]
                .getDeclaredConstructor().newInstance();
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> leaf1Pc = ProcessedCommandBuilder
                .<Command<CommandInvocation>, CommandInvocation> builder()
                .name(name + "-leaf1")
                .description("Leaf 1 of " + name)
                .command(leaf1Cmd)
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('n')
                        .name("name")
                        .description("Name")
                        .type(String.class)
                        .fieldName("output")
                        .optionType(OptionType.NORMAL)
                        .build())
                .addOption(ProcessedOptionBuilder.builder()
                        .name("force")
                        .description("Force")
                        .type(boolean.class)
                        .fieldName("recursive")
                        .optionType(OptionType.BOOLEAN)
                        .build())
                .create();
        leaf1Pc.setArgument(ProcessedOptionBuilder.builder()
                .shortName('\u0000')
                .name("")
                .description("Value")
                .type(String.class)
                .fieldName("source")
                .optionType(OptionType.ARGUMENT)
                .build());
        AeshCommandContainer<CommandInvocation> leaf1Container = new AeshCommandContainer<>(leaf1Pc);

        // Leaf 2
        Command<CommandInvocation> leaf2Cmd = AESH_COMMANDS[(index + 3) % AESH_COMMANDS.length]
                .getDeclaredConstructor().newInstance();
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> leaf2Pc = ProcessedCommandBuilder
                .<Command<CommandInvocation>, CommandInvocation> builder()
                .name(name + "-leaf2")
                .description("Leaf 2 of " + name)
                .command(leaf2Cmd)
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('f')
                        .name("file")
                        .description("File path")
                        .type(String.class)
                        .fieldName("output")
                        .optionType(OptionType.NORMAL)
                        .build())
                .addOption(ProcessedOptionBuilder.builder()
                        .name("encoding")
                        .description("Encoding")
                        .type(String.class)
                        .fieldName("source")
                        .optionType(OptionType.NORMAL)
                        .addDefaultValue("UTF-8")
                        .build())
                .create();
        leaf2Pc.setArgument(ProcessedOptionBuilder.builder()
                .shortName('\u0000')
                .name("")
                .description("Data")
                .type(String.class)
                .fieldName("source")
                .optionType(OptionType.ARGUMENT)
                .build());
        AeshCommandContainer<CommandInvocation> leaf2Container = new AeshCommandContainer<>(leaf2Pc);

        // Wire: leaves -> mid -> top
        midContainer.addChild(leaf1Container);
        midContainer.addChild(leaf2Container);
        topContainer.addChild(midContainer);

        return topContainer;
    }

    private void buildAeshNestedReflection(int count) throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        AeshCommandContainerBuilder<CommandInvocation> containerBuilder = new AeshCommandContainerBuilder<>();

        // Use the single AeshTopGroup class (contains 3-level hierarchy via annotations)
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                Command<CommandInvocation> cmd = (Command<CommandInvocation>) new AeshTopGroup();
                CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) REFLECTION_CREATE
                        .invoke(containerBuilder, cmd);
                registry.addCommand(container);
            } else {
                registry.addCommand(createGeneratedNestedGroupContainer("nested-" + i, i));
            }
        }

        AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();
    }

    private void buildPicocliNestedProgrammatic(int count) {
        CommandSpec mainSpec = CommandSpec.create();
        mainSpec.name("main");
        CommandLine cmd = new CommandLine(mainSpec);
        for (int i = 0; i < count; i++) {
            cmd.addSubcommand("nested-" + i, createPicocliProgrammaticNestedGroup("nested-" + i));
        }
    }

    private CommandLine createPicocliProgrammaticNestedGroup(String name) {
        // Top
        CommandSpec topSpec = CommandSpec.create();
        topSpec.name(name);
        topSpec.addOption(OptionSpec.builder("-g", "--global")
                .type(boolean.class)
                .description("Global flag")
                .build());
        CommandLine top = new CommandLine(topSpec);

        // Mid
        CommandSpec midSpec = CommandSpec.create();
        midSpec.name(name + "-mid");
        midSpec.addOption(OptionSpec.builder("-r", "--region")
                .type(String.class)
                .description("Region")
                .build());
        CommandLine mid = new CommandLine(midSpec);

        // Leaf 1
        CommandSpec leaf1Spec = CommandSpec.create();
        leaf1Spec.name(name + "-leaf1");
        leaf1Spec.addOption(OptionSpec.builder("-n", "--name")
                .type(String.class)
                .description("Name")
                .build());
        leaf1Spec.addOption(OptionSpec.builder("--force")
                .type(boolean.class)
                .description("Force")
                .build());
        leaf1Spec.addPositional(PositionalParamSpec.builder()
                .type(String.class)
                .description("Value")
                .build());
        mid.addSubcommand(name + "-leaf1", new CommandLine(leaf1Spec));

        // Leaf 2
        CommandSpec leaf2Spec = CommandSpec.create();
        leaf2Spec.name(name + "-leaf2");
        leaf2Spec.addOption(OptionSpec.builder("-f", "--file")
                .type(String.class)
                .description("File path")
                .build());
        leaf2Spec.addOption(OptionSpec.builder("--encoding")
                .type(String.class)
                .description("Encoding")
                .defaultValue("UTF-8")
                .build());
        leaf2Spec.addPositional(PositionalParamSpec.builder()
                .type(String.class)
                .description("Data")
                .build());
        mid.addSubcommand(name + "-leaf2", new CommandLine(leaf2Spec));

        top.addSubcommand(name + "-mid", mid);
        return top;
    }

    private void buildPicocliNestedReflection(int count) {
        CommandLine cmd = new CommandLine(new PicocliMain());
        for (int i = 0; i < count; i++) {
            try {
                cmd.addSubcommand("nested-" + i, new PicoTopGroup());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // ---- Option variety benchmark (OptionList, OptionGroup, generateHelp, version) ----

    public void runOptionVarietyBenchmark() throws Exception {
        System.out.println("=== Option Variety Benchmark (OptionList, OptionGroup, generateHelp, version) ===");
        System.out.println("Warmup: " + WARMUP_ITERATIONS + ", Measured: " + MEASURED_ITERATIONS + " iterations");
        System.out.println();

        double[] genResults = new double[COMMAND_COUNTS.length];
        double[] reflResults = new double[COMMAND_COUNTS.length];
        double[] picoProgResults = new double[COMMAND_COUNTS.length];
        double[] picoReflResults = new double[COMMAND_COUNTS.length];

        for (int c = 0; c < COMMAND_COUNTS.length; c++) {
            int count = COMMAND_COUNTS[c];

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildAeshVarietyGenerated(count);
            }
            long genNs = timeIterations(() -> buildAeshVarietyGenerated(count));
            genResults[c] = (genNs / (double) MEASURED_ITERATIONS) / 1000.0;

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildAeshVarietyReflection(count);
            }
            long reflNs = timeIterations(() -> buildAeshVarietyReflection(count));
            reflResults[c] = (reflNs / (double) MEASURED_ITERATIONS) / 1000.0;

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildPicocliVarietyProgrammatic(count);
            }
            long picoProgNs = timeIterations(() -> buildPicocliVarietyProgrammatic(count));
            picoProgResults[c] = (picoProgNs / (double) MEASURED_ITERATIONS) / 1000.0;

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                buildPicocliVarietyReflection(count);
            }
            long picoReflNs = timeIterations(() -> buildPicocliVarietyReflection(count));
            picoReflResults[c] = (picoReflNs / (double) MEASURED_ITERATIONS) / 1000.0;
        }

        System.out.printf("Commands | %14s | %14s | %14s | %14s | Refl/Gen | PicoR/Gen%n",
                "Aesh gen (us)", "Aesh refl (us)", "Pico prog (us)", "Pico refl (us)");
        System.out.println("---------|-" + "-".repeat(14) + "-|-" + "-".repeat(14) + "-|-"
                + "-".repeat(14) + "-|-" + "-".repeat(14) + "-|----------|----------");
        for (int c = 0; c < COMMAND_COUNTS.length; c++) {
            double reflGenRatio = genResults[c] != 0 ? reflResults[c] / genResults[c] : 0;
            double picoReflGenRatio = genResults[c] != 0 ? picoReflResults[c] / genResults[c] : 0;
            System.out.printf("%8d | %14.1f | %14.1f | %14.1f | %14.1f | %7.2fx | %8.2fx%n",
                    COMMAND_COUNTS[c], genResults[c], reflResults[c],
                    picoProgResults[c], picoReflResults[c],
                    reflGenRatio, picoReflGenRatio);
        }
    }

    private void buildAeshVarietyGenerated(int commandCount) throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        for (int i = 0; i < commandCount; i++) {
            registry.addCommand(createGeneratedVarietyContainer("vcmd-" + i, i));
        }
        AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();
    }

    private CommandContainer<CommandInvocation> createGeneratedVarietyContainer(String name, int index)
            throws Exception {
        Command<CommandInvocation> cmd = AESH_COMMANDS[index % AESH_COMMANDS.length]
                .getDeclaredConstructor().newInstance();
        ProcessedCommandBuilder<Command<CommandInvocation>, CommandInvocation> builder = ProcessedCommandBuilder
                .<Command<CommandInvocation>, CommandInvocation> builder()
                .name(name)
                .description("Variety " + name)
                .command(cmd)
                .generateHelp(index % 2 == 0)
                .version(index % 3 == 0 ? "1.0.0" : "")
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('i')
                        .name("items")
                        .description("Items list")
                        .type(String.class)
                        .fieldName("output")
                        .optionType(OptionType.LIST)
                        .valueSeparator(',')
                        .build())
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('D')
                        .name("properties")
                        .description("Properties")
                        .type(String.class)
                        .fieldName("source")
                        .optionType(OptionType.GROUP)
                        .build())
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('v')
                        .name("verbose")
                        .description("Verbose")
                        .type(boolean.class)
                        .fieldName("recursive")
                        .optionType(OptionType.BOOLEAN)
                        .build());
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> pc = builder.create();
        pc.setArgument(ProcessedOptionBuilder.builder()
                .shortName('\u0000')
                .name("")
                .description("Target")
                .type(String.class)
                .fieldName("source")
                .optionType(OptionType.ARGUMENT)
                .build());
        return new AeshCommandContainer<>(pc);
    }

    private void buildAeshVarietyReflection(int commandCount) throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        AeshCommandContainerBuilder<CommandInvocation> containerBuilder = new AeshCommandContainerBuilder<>();

        int annotatedCount = Math.min(commandCount, AESH_VARIETY_COMMANDS.length);
        for (int i = 0; i < annotatedCount; i++) {
            Command<CommandInvocation> cmd = AESH_VARIETY_COMMANDS[i].getDeclaredConstructor().newInstance();
            CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) REFLECTION_CREATE
                    .invoke(containerBuilder, cmd);
            registry.addCommand(container);
        }

        for (int i = AESH_VARIETY_COMMANDS.length; i < commandCount; i++) {
            registry.addCommand(createGeneratedVarietyContainer("vcmd-" + i, i));
        }

        AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();
    }

    private void buildPicocliVarietyProgrammatic(int commandCount) {
        CommandSpec mainSpec = CommandSpec.create();
        mainSpec.name("main");
        CommandLine cmd = new CommandLine(mainSpec);
        for (int i = 0; i < commandCount; i++) {
            cmd.addSubcommand("vcmd-" + i, new CommandLine(createPicocliProgrammaticVariety("vcmd-" + i, i)));
        }
    }

    private CommandSpec createPicocliProgrammaticVariety(String name, int index) {
        CommandSpec spec = CommandSpec.create();
        spec.name(name);
        if (index % 2 == 0) {
            spec.mixinStandardHelpOptions(true);
        }
        if (index % 3 == 0) {
            spec.version("1.0.0");
        }
        spec.addOption(OptionSpec.builder("-i", "--items")
                .type(List.class)
                .splitRegex(",")
                .description("Items list")
                .build());
        spec.addOption(OptionSpec.builder("-D")
                .type(Map.class)
                .description("Properties")
                .build());
        spec.addOption(OptionSpec.builder("-v", "--verbose")
                .type(boolean.class)
                .description("Verbose")
                .build());
        spec.addPositional(PositionalParamSpec.builder()
                .type(String.class)
                .description("Target")
                .build());
        return spec;
    }

    private void buildPicocliVarietyReflection(int commandCount) {
        CommandLine cmd = new CommandLine(new PicocliMain());
        for (int i = 0; i < commandCount; i++) {
            Class<? extends Runnable> picoClass = PICO_VARIETY_COMMANDS[i % PICO_VARIETY_COMMANDS.length];
            try {
                cmd.addSubcommand("vcmd-" + i, picoClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // ---- Parsing benchmark ----

    public void runParsingBenchmark() throws Exception {
        System.out.println("=== Parsing Benchmark ===");
        System.out.println("Measuring time to parse a command line (containers pre-built)");
        System.out.println("Warmup: " + WARMUP_ITERATIONS + ", Measured: " + MEASURED_ITERATIONS + " iterations");
        System.out.println();

        AeshCommandContainerBuilder<CommandInvocation> containerBuilder = new AeshCommandContainerBuilder<>();

        // Test cases: {label, aesh parse line, picocli args...}
        String[] labels = { "Flat (4 opts)", "List+Group opts", "Subcommand" };

        // Aesh parsers and parse lines
        CommandLineParser<CommandInvocation> aeshFlatParser = containerBuilder
                .create(new AeshCmd1()).getParser();
        CommandLineParser<CommandInvocation> aeshListParser = containerBuilder
                .create(new AeshListGroupCmd()).getParser();
        CommandLineParser<CommandInvocation> aeshGroupParser = containerBuilder
                .create(new AeshGrpCmd1()).getParser();
        CommandLineParser<CommandInvocation>[] aeshParsers = new CommandLineParser[] {
                aeshFlatParser, aeshListParser, aeshGroupParser
        };
        String[] aeshLines = {
                "file-ops -o /tmp/out -r --bufferSize 8192 source.txt",
                "list-group-cmd -i a,b,c,d,e -Dfoo=bar -Dbaz=qux -v target",
                "grp-1 child-1a -i input.txt src"
        };

        // Picocli instances and args
        String[][] picoArgs = {
                { "-o", "/tmp/out", "-r", "--buffer-size", "8192", "source.txt" },
                { "-i", "a,b,c,d,e", "-Dfoo=bar", "-Dbaz=qux", "-v", "target" },
                { "child-1a", "-i", "input.txt", "src" }
        };
        Class<?>[] picoClasses = { PicoCmd1.class, PicoListGroupCmd.class, PicoGrpCmd1.class };

        System.out.printf("%-18s | %14s | %14s | %s%n", "Pattern", "Aesh (us)", "Picocli (us)", "Ratio");
        System.out.println("-".repeat(18) + "-|-" + "-".repeat(14) + "-|-" + "-".repeat(14) + "-|--------");

        for (int t = 0; t < labels.length; t++) {
            final CommandLineParser<CommandInvocation> ap = aeshParsers[t];
            final String al = aeshLines[t];

            // Warmup + measure aesh parsing
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                ap.parse(al, CommandLineParser.Mode.STRICT);
            }
            long aeshNs = timeIterations(() -> ap.parse(al, CommandLineParser.Mode.STRICT));
            double aeshUs = (aeshNs / (double) MEASURED_ITERATIONS) / 1000.0;

            // Picocli: reuse CommandLine instance, parseArgs overwrites previous results
            final CommandLine picoCmd = new CommandLine(picoClasses[t].getDeclaredConstructor().newInstance());
            final String[] pa = picoArgs[t];
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                picoCmd.parseArgs(pa);
            }
            long picoNs = timeIterations(() -> picoCmd.parseArgs(pa));
            double picoUs = (picoNs / (double) MEASURED_ITERATIONS) / 1000.0;

            double ratio = aeshUs != 0 ? picoUs / aeshUs : 0;
            System.out.printf("%-18s | %14.2f | %14.2f | %5.2fx%n", labels[t], aeshUs, picoUs, ratio);
        }
    }

    // ---- Completion parsing benchmark (aesh only) ----

    public void runCompletionBenchmark() throws Exception {
        System.out.println("=== Completion Parsing Benchmark (Aesh only) ===");
        System.out.println("Measuring time to parse in COMPLETION mode");
        System.out.println("Warmup: " + WARMUP_ITERATIONS + ", Measured: " + MEASURED_ITERATIONS + " iterations");
        System.out.println();

        AeshCommandContainerBuilder<CommandInvocation> containerBuilder = new AeshCommandContainerBuilder<>();

        String[] labels = {
                "Option name",
                "Option value",
                "Mid-command",
                "Subcommand"
        };

        CommandLineParser<CommandInvocation> flatParser = containerBuilder
                .create(new AeshCmd1()).getParser();
        CommandLineParser<CommandInvocation> groupParser = containerBuilder
                .create(new AeshGrpCmd1()).getParser();

        CommandLineParser<CommandInvocation>[] parsers = new CommandLineParser[] {
                flatParser, flatParser, flatParser, groupParser
        };
        String[] lines = {
                "file-ops --ou",
                "file-ops -o /tmp/",
                "file-ops -o /tmp/out -r --buffer",
                "grp-1 child-1a -i inp"
        };

        System.out.printf("%-18s | %14s%n", "Pattern", "Aesh (us)");
        System.out.println("-".repeat(18) + "-|-" + "-".repeat(14));

        for (int t = 0; t < labels.length; t++) {
            final CommandLineParser<CommandInvocation> parser = parsers[t];
            final String line = lines[t];

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                parser.parse(line, CommandLineParser.Mode.COMPLETION);
            }
            long ns = timeIterations(() -> parser.parse(line, CommandLineParser.Mode.COMPLETION));
            double us = (ns / (double) MEASURED_ITERATIONS) / 1000.0;

            System.out.printf("%-18s | %14.2f%n", labels[t], us);
        }
    }

    // ---- Timing infrastructure ----

    private long timeIterations(ThrowingRunnable runnable) throws Exception {
        long start = System.nanoTime();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            runnable.run();
        }
        return System.nanoTime() - start;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
