package org.jboss.aesh.console;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class MutableCommandRegistry implements CommandRegistry {

    private Map<String,CommandContainer> registry = new HashMap<String, CommandContainer>();

    @Override
    public Command getCommand(String name) throws CommandNotFoundException {
        if(registry.containsKey(name))
            return registry.get(name).getCommand();
        else
            throw new CommandNotFoundException("Command: "+name+" was not found.");
    }

    @Override
    public Map<String, CommandContainer> asMap() {
        return registry;
    }

    public void addCommand(Command command) {
        CommandContainer commandContainer = new CommandContainer(command);
        if(!commandContainer.hasError() &&
                !registry.containsKey(commandContainer.getParser().getCommand().getName()))
            registry.put(commandContainer.getParser().getCommand().getName(), commandContainer);
    }

    public void addCommand(Class<? extends Command> command) {
        CommandContainer commandContainer = new CommandContainer(command);
        if(!commandContainer.hasError() &&
                !registry.containsKey(commandContainer.getParser().getCommand().getName()))
            registry.put(commandContainer.getParser().getCommand().getName(), commandContainer);
    }

    public void removeCommand(String name) {
        if(registry.containsKey(name))
            registry.remove(name);
    }

}
