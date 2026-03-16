package org.aesh.command;

import java.lang.reflect.Method;

import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.OptionType;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.registry.MutableCommandRegistryImpl;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.junit.Test;

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
 *
 * @author Aesh team
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class StartupBenchmarkTest {

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

    @CommandDefinition(name = "file-ops", description = "File operations")
    public static class AeshCmd1 implements Command<CommandInvocation> {
        @Option(shortName = 'o', description = "Output path")
        private String output;
        @Option(shortName = 'r', hasValue = false, description = "Recursive")
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

    @CommandDefinition(name = "net-fetch", description = "Network fetch")
    public static class AeshCmd2 implements Command<CommandInvocation> {
        @Option(shortName = 'u', description = "URL")
        private String url;
        @Option(shortName = 'v', hasValue = false, description = "Verbose")
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
        @Option(shortName = 'p', description = "Port", defaultValue = "5432")
        private int port;
        @Option(shortName = 'q', hasValue = false, description = "Quiet")
        private boolean quiet;
        @Argument(description = "Query string")
        private String query;

        @Override
        public CommandResult execute(CommandInvocation ci) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "build-proj", description = "Build project")
    public static class AeshCmd4 implements Command<CommandInvocation> {
        @Option(shortName = 't', description = "Target")
        private String target;
        @Option(hasValue = false, description = "Clean first")
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

    @CommandDefinition(name = "deploy-app", description = "Deploy application")
    public static class AeshCmd5 implements Command<CommandInvocation> {
        @Option(shortName = 'e', description = "Environment")
        private String env;
        @Option(shortName = 'd', hasValue = false, description = "Dry run")
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
        @Option(shortName = 'i', hasValue = false, description = "Case insensitive")
        private boolean ignoreCase;
        @Option(description = "Max results", defaultValue = "100")
        private int maxResults;
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
        @Option(shortName = 'f', description = "Format")
        private String format;
        @Option(shortName = 'v', hasValue = false, description = "Verbose")
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
            AeshGrpChild2b.class })
    public static class AeshGrpCmd2 implements Command<CommandInvocation> {
        @Option(shortName = 'e', description = "Environment")
        private String env;
        @Option(shortName = 'q', hasValue = false, description = "Quiet")
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
            AeshGrpChild3b.class })
    public static class AeshGrpCmd3 implements Command<CommandInvocation> {
        @Option(shortName = 'l', description = "Level")
        private String level;
        @Option(shortName = 'd', hasValue = false, description = "Dry run")
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

    // ---- Benchmark tests ----

    @Test
    public void startupBenchmark() throws Exception {
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

    @Test
    public void groupCommandBenchmark() throws Exception {
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
    // Simulates what the generated _AeshMetadata code does at runtime:
    // builds ProcessedCommand via ProcessedCommandBuilder with hardcoded metadata (no reflection).

    private void buildAeshRuntimeGenerated(int commandCount) throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        for (int i = 0; i < commandCount; i++) {
            registry.addCommand(createGeneratedContainer("cmd-" + i, i));
        }
        AeshCommandRuntimeBuilder.builder().commandRegistry(registry).build();
    }

    /**
     * Creates a CommandContainer the same way generated metadata code would:
     * using ProcessedCommandBuilder + ProcessedOptionBuilder with literal values.
     * Each command gets 3 options (String, boolean, int) + 1 argument (String).
     */
    private CommandContainer<CommandInvocation> createGeneratedContainer(String name, int index) throws Exception {
        Command<CommandInvocation> cmd = AESH_COMMANDS[index % AESH_COMMANDS.length]
                .getDeclaredConstructor().newInstance();
        ProcessedCommand<Command<CommandInvocation>, CommandInvocation> pc = ProcessedCommandBuilder
                .<Command<CommandInvocation>, CommandInvocation> builder()
                .name(name)
                .description("Command " + name)
                .command(cmd)
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('o')
                        .name("output")
                        .description("Output path")
                        .type(String.class)
                        .fieldName("output")
                        .optionType(OptionType.NORMAL)
                        .build())
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

    // ---- Aesh: reflection path (flat) ----
    // Uses the actual annotation-scanning code path (doGenerateCommandLineParser)
    // which reads @CommandDefinition, @Option, @Argument via reflection.

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

    /**
     * Creates a CommandSpec programmatically with 3 options + 1 positional param,
     * mirroring the structure of the annotated PicoCmd classes.
     */
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

    /**
     * Creates a group CommandContainer with 2 child subcommands, all built via
     * ProcessedCommandBuilder (no annotation reflection). Parent gets 2 options,
     * each child gets 2 options + 1 argument.
     */
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
                        .build())
                .addOption(ProcessedOptionBuilder.builder()
                        .shortName('v')
                        .name("verbose")
                        .description("Verbose")
                        .type(boolean.class)
                        .fieldName("recursive")
                        .optionType(OptionType.BOOLEAN)
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

    /**
     * Creates a picocli group (parent + 2 child subcommands) programmatically.
     * Parent gets 2 options, each child gets 2 options + 1 positional param.
     */
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
