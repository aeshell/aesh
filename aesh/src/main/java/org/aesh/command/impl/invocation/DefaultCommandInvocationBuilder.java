package org.aesh.command.impl.invocation;

import java.util.function.Consumer;

import org.aesh.command.CommandRuntime;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.operator.OutputDelegate;
import org.aesh.command.impl.shell.ShellOutputDelegate;
import org.aesh.command.impl.shell.ShellOutputTee;
import org.aesh.command.impl.shell.StdoutShell;
import org.aesh.command.invocation.CommandInvocationBuilder;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.command.shell.Shell;
import org.aesh.command.status.ProgramStatusScope;

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
        if (outputHandler != null) {
            // Tee even without a shell: fall back to System.out so the
            // handler receives what aesh prints itself (like --help)
            // instead of silently getting nothing (#769).
            Shell base = effectiveShell != null ? effectiveShell : new StdoutShell();
            effectiveShell = new ShellOutputTee(base, outputHandler);
        }
        OutputDelegate redirection = configuration == null ? null : configuration.getOutputRedirection();
        if (redirection != null)
            effectiveShell = new ShellOutputDelegate(effectiveShell, redirection);
        DefaultCommandInvocation invocation = new DefaultCommandInvocation(runtime, configuration, commandContainer,
                effectiveShell);
        invocation.setProgramStatusScope(ProgramStatusScope.current());
        return invocation;
    }
}
