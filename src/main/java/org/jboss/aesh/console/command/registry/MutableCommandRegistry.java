package org.jboss.aesh.console.command.registry;

import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.container.AeshCommandContainerBuilder;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.container.CommandContainerBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class MutableCommandRegistry implements CommandRegistry {

    private Map<String,CommandContainer> registry = new HashMap<String, CommandContainer>();

    private CommandContainerBuilder containerBuilder;

    public void setCommandContainerBuilder(CommandContainerBuilder containerBuilder) {
        this.containerBuilder = containerBuilder;
    }

    @Override
    public CommandContainer getCommand(String name, String line) throws CommandNotFoundException {
        if(registry.containsKey(name))
            return registry.get(name);
        else
            throw new CommandNotFoundException("Command: "+name+" was not found.");
    }

    @Override
    public Set<String> getAllCommandNames() {
        return registry.keySet();
    }

    public void addCommand(CommandContainer container) {
        putIntoRegistry(container);
    }

    public void addCommand(Command command) {
        putIntoRegistry(getBuilder().build(command));
    }

    public void addCommand(Class<? extends Command> command) {
        putIntoRegistry(getBuilder().build(command));
    }

    private void putIntoRegistry(CommandContainer commandContainer) {
        if(!commandContainer.haveBuildError() &&
                !registry.containsKey(commandContainer.getParser().getCommand().getName()))
            registry.put(commandContainer.getParser().getCommand().getName(), commandContainer);
    }

    public void removeCommand(String name) {
        if(registry.containsKey(name))
            registry.remove(name);
    }

    private CommandContainerBuilder getBuilder() {
        if(containerBuilder == null)
            containerBuilder = new AeshCommandContainerBuilder();
        return containerBuilder;
    }

}
