package org.jboss.aesh.console.command;

import org.jboss.aesh.cl.parser.CommandLineParser;

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
