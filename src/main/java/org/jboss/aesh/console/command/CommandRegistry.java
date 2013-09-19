package org.jboss.aesh.console.command;

import java.util.Set;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CommandRegistry {

    CommandContainer getCommand(String name, String completeLine) throws CommandNotFoundException;

    Set<String> getAllCommandNames();
}
