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

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ProfileStartup {

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

    private static final Class<? extends Command>[] COMMANDS = new Class[] {
            StartupBenchmark.AeshCmd1.class, StartupBenchmark.AeshCmd2.class,
            StartupBenchmark.AeshCmd3.class, StartupBenchmark.AeshCmd4.class,
            StartupBenchmark.AeshCmd5.class, StartupBenchmark.AeshCmd6.class,
            StartupBenchmark.AeshCmd7.class, StartupBenchmark.AeshCmd8.class,
            StartupBenchmark.AeshCmd9.class, StartupBenchmark.AeshCmd10.class
    };

    public static void main(String[] args) throws Exception {
        String mode = "both";
        int iterations = 10_000;
        if (args.length > 0)
            mode = args[0];
        if (args.length > 1)
            iterations = Integer.parseInt(args[1]);

        if ("reflection".equals(mode) || "both".equals(mode)) {
            System.out.println("=== Reflection path ===");
            System.out.println("Warming up (1000 iterations)...");
            for (int i = 0; i < 1000; i++)
                buildReflection();
            System.out.println("Profiling " + iterations + " iterations...");
            for (int i = 0; i < iterations; i++)
                buildReflection();
            System.out.println("Done reflection.");
        }

        if ("generated".equals(mode) || "both".equals(mode)) {
            System.out.println("=== Generated path ===");
            System.out.println("Warming up (1000 iterations)...");
            for (int i = 0; i < 1000; i++)
                buildGenerated();
            System.out.println("Profiling " + iterations + " iterations...");
            for (int i = 0; i < iterations; i++)
                buildGenerated();
            System.out.println("Done generated.");
        }
    }

    private static void buildReflection() throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        AeshCommandContainerBuilder<CommandInvocation> containerBuilder = new AeshCommandContainerBuilder<>();
        for (Class<? extends Command> clazz : COMMANDS) {
            Command<CommandInvocation> cmd = clazz.getDeclaredConstructor().newInstance();
            CommandContainer<CommandInvocation> container = (CommandContainer<CommandInvocation>) REFLECTION_CREATE
                    .invoke(containerBuilder, cmd);
            registry.addCommand(container);
        }
    }

    private static void buildGenerated() throws Exception {
        MutableCommandRegistryImpl<CommandInvocation> registry = new MutableCommandRegistryImpl<>();
        for (int i = 0; i < 10; i++) {
            registry.addCommand(createGeneratedContainer("cmd-" + i, i));
        }
    }

    private static CommandContainer<CommandInvocation> createGeneratedContainer(String name, int index) throws Exception {
        Command<CommandInvocation> cmd = COMMANDS[index % COMMANDS.length]
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
}
