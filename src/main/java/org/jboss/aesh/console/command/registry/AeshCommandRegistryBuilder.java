package org.jboss.aesh.console.command.registry;

import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.container.AeshCommandContainer;
import org.jboss.aesh.console.command.container.CommandContainerBuilder;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCommandRegistryBuilder {

    private MutableCommandRegistry commandRegistry;

    public AeshCommandRegistryBuilder() {
        commandRegistry = new MutableCommandRegistry();
    }

    public AeshCommandRegistryBuilder containerBuilder(CommandContainerBuilder builder) {
        commandRegistry.setCommandContainerBuilder(builder);
        return this;
    }

    public AeshCommandRegistryBuilder command(Class<? extends Command> command) {
        commandRegistry.addCommand(command);
        return this;
    }

    public AeshCommandRegistryBuilder command(ProcessedCommand processedCommand,
            Class<? extends Command> command) {
        commandRegistry.addCommand(new AeshCommandContainer(processedCommand, command));
        return this;
    }

    public AeshCommandRegistryBuilder command(ProcessedCommand processedCommand,
            Command command) {
        commandRegistry.addCommand(new AeshCommandContainer(processedCommand, command));
        return this;
    }

     public AeshCommandRegistryBuilder command(CommandLineParser parser,
            Class<? extends Command> command) {
        commandRegistry.addCommand(new AeshCommandContainer(parser, command));
        return this;
    }

    public AeshCommandRegistryBuilder command(CommandLineParser parser,
            Command command) {
        commandRegistry.addCommand(new AeshCommandContainer(parser, command));
        return this;
    }

    public AeshCommandRegistryBuilder command(Command command) {
        commandRegistry.addCommand(command);
        return this;
    }

    public CommandRegistry create() {
        return commandRegistry;
    }

}
