package org.aesh.command.impl.invocation;

import java.util.function.Consumer;

import org.aesh.command.CommandRuntime;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.shell.ShellOutputTee;
import org.aesh.command.invocation.CommandInvocationBuilder;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.command.shell.Shell;

public class DefaultCommandInvocationBuilder implements CommandInvocationBuilder<DefaultCommandInvocation> {

    private final Shell shell;
    private final Consumer<String> outputHandler;

    public DefaultCommandInvocationBuilder(Shell shell) {
        this(shell, null);
    }

    public DefaultCommandInvocationBuilder(Shell shell, Consumer<String> outputHandler) {
        this.shell = shell;
        this.outputHandler = outputHandler;
    }

    @Override
    public DefaultCommandInvocation build(CommandRuntime<DefaultCommandInvocation> runtime,
            CommandInvocationConfiguration configuration,
            CommandContainer<DefaultCommandInvocation> commandContainer) {
        Shell effectiveShell = shell;
        if (outputHandler != null && effectiveShell != null)
            effectiveShell = new ShellOutputTee(effectiveShell, outputHandler);
        return new DefaultCommandInvocation(runtime, configuration, commandContainer, effectiveShell);
    }
}
