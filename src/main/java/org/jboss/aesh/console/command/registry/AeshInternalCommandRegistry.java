package org.jboss.aesh.console.command.registry;

import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.container.AeshCommandContainerBuilder;
import org.jboss.aesh.console.command.container.CommandContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Only used by AeshConsoleImpl to store built-in commands
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshInternalCommandRegistry {

    private final Map<String, CommandContainer> registry = new HashMap<String, CommandContainer>();

    public void addCommand(Command command) {
        putIntoRegistry(new AeshCommandContainerBuilder().build(command));
    }

    private void putIntoRegistry(CommandContainer commandContainer) {
        if(!commandContainer.haveBuildError() &&
                !registry.containsKey(commandContainer.getParser().getCommand().getName()))
            registry.put(commandContainer.getParser().getCommand().getName(), commandContainer);
    }

    public CommandContainer getCommand(String name) {
        return registry.get(name);
    }

    public Set<String> getAllCommandNames() {
        return registry.keySet();
    }
}
